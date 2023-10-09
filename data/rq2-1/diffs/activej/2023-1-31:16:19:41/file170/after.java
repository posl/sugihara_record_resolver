package io.activej.cube.http;

import io.activej.aggregation.*;
import io.activej.aggregation.annotation.Key;
import io.activej.aggregation.annotation.Measures;
import io.activej.aggregation.fieldtype.FieldType;
import io.activej.aggregation.measure.Measure;
import io.activej.async.function.AsyncSupplier;
import io.activej.common.ref.RefLong;
import io.activej.csp.process.frames.LZ4FrameFormat;
import io.activej.cube.*;
import io.activej.cube.attributes.AbstractAttributeResolver;
import io.activej.cube.ot.CubeDiff;
import io.activej.datastream.StreamConsumer;
import io.activej.datastream.StreamDataAcceptor;
import io.activej.datastream.StreamSupplier;
import io.activej.etl.LogDiff;
import io.activej.etl.LogOTProcessor;
import io.activej.etl.LogOTState;
import io.activej.etl.SplitterLogDataConsumer;
import io.activej.fs.FileSystem;
import io.activej.http.HttpClient;
import io.activej.http.HttpServer;
import io.activej.http.IHttpClient;
import io.activej.multilog.IMultilog;
import io.activej.multilog.Multilog;
import io.activej.ot.OTStateManager;
import io.activej.ot.uplink.AsyncOTUplink;
import io.activej.reactor.Reactor;
import io.activej.record.Record;
import io.activej.serializer.SerializerFactory;
import io.activej.serializer.annotations.Serialize;
import org.jetbrains.annotations.Nullable;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Stream;

import static io.activej.aggregation.fieldtype.FieldTypes.*;
import static io.activej.aggregation.measure.Measures.*;
import static io.activej.aggregation.predicate.AggregationPredicates.*;
import static io.activej.common.Utils.concat;
import static io.activej.common.Utils.entriesToMap;
import static io.activej.cube.ComputedMeasures.*;
import static io.activej.cube.Cube.AggregationConfig.id;
import static io.activej.cube.CubeQuery.Ordering.asc;
import static io.activej.cube.ReportType.DATA;
import static io.activej.cube.ReportType.DATA_WITH_TOTALS;
import static io.activej.cube.http.ReportingTest.LogItem.*;
import static io.activej.multilog.LogNamingScheme.NAME_PARTITION_REMAINDER_SEQ;
import static io.activej.promise.TestUtils.await;
import static io.activej.test.TestUtils.getFreePort;
import static java.util.stream.Collectors.toSet;
import static org.junit.Assert.*;

@SuppressWarnings("rawtypes")
public final class ReportingTest extends CubeTestBase {
	public static final double DELTA = 1E-3;

	private HttpServer cubeHttpServer;
	private HttpClientCube cubeHttp;
	private Cube cube;
	private int serverPort;

	private static final Map<String, FieldType> DIMENSIONS_CUBE = entriesToMap(Stream.of(
			Map.entry("date", ofLocalDate(LocalDate.parse("2000-01-01"))),
			Map.entry("advertiser", ofInt()),
			Map.entry("campaign", ofInt()),
			Map.entry("banner", ofInt()),
			Map.entry("affiliate", ofInt()),
			Map.entry("site", ofString())));

	private static final Map<String, FieldType> DIMENSIONS_DATE_AGGREGATION =
			Map.of("date", ofLocalDate(LocalDate.parse("2000-01-01")));

	private static final Map<String, FieldType> DIMENSIONS_ADVERTISERS_AGGREGATION = entriesToMap(Stream.of(
			Map.entry("date", ofLocalDate(LocalDate.parse("2000-01-01"))),
			Map.entry("advertiser", ofInt()),
			Map.entry("campaign", ofInt()),
			Map.entry("banner", ofInt())));

	private static final Map<String, FieldType> DIMENSIONS_AFFILIATES_AGGREGATION = entriesToMap(Stream.of(
			Map.entry("date", ofLocalDate(LocalDate.parse("2000-01-01"))),
			Map.entry("affiliate", ofInt()),
			Map.entry("site", ofString())));

	private static final Map<String, Measure> MEASURES = entriesToMap(Stream.of(
			Map.entry("impressions", sum(ofLong())),
			Map.entry("clicks", sum(ofLong())),
			Map.entry("conversions", sum(ofLong())),
			Map.entry("revenue", sum(ofDouble())),
			Map.entry("eventCount", count(ofInt())),
			Map.entry("minRevenue", min(ofDouble())),
			Map.entry("maxRevenue", max(ofDouble())),
			Map.entry("uniqueUserIdsCount", hyperLogLog(1024)),
			Map.entry("errors", sum(ofLong()))));

	private static class AdvertiserAttributeResolver extends AbstractAttributeResolver<Integer, String> {
		public AdvertiserAttributeResolver(Reactor reactor) {
			super(reactor);
		}

		@Override
		public Class<?>[] getKeyTypes() {
			return new Class[]{int.class};
		}

		@Override
		protected Integer toKey(Object[] keyArray) {
			return (int) keyArray[0];
		}

		@Override
		public Map<String, Class<?>> getAttributeTypes() {
			return Map.of("name", String.class);
		}

		@Override
		protected Object[] toAttributes(String attributes) {
			return new Object[]{attributes};
		}

		@Override
		public @Nullable String resolveAttributes(Integer key) {
			return switch (key) {
				case 1 -> "first";
				case 3 -> "third";
				default -> null;
			};
		}
	}

	@Measures("eventCount")
	public static class LogItem {
		static final int EXCLUDE_AFFILIATE = 0;
		static final String EXCLUDE_SITE = "--";

		static final int EXCLUDE_ADVERTISER = 0;
		static final int EXCLUDE_CAMPAIGN = 0;
		static final int EXCLUDE_BANNER = 0;

		@Key
		@Serialize
		public int date;

		@Key
		@Serialize
		public int advertiser;

		@Key
		@Serialize
		public int campaign;

		@Key
		@Serialize
		public int banner;

		@Key
		@Serialize
		public int affiliate;

		@Key
		@Serialize
		public String site;

		@Measures
		@Serialize
		public long impressions;

		@Measures
		@Serialize
		public long clicks;

		@Measures
		@Serialize
		public long conversions;

		@Measures({"minRevenue", "maxRevenue", "revenue"})
		@Serialize
		public double revenue;

		@Measures("uniqueUserIdsCount")
		@Serialize
		public int userId;

		@Measures
		@Serialize
		public int errors;

		@SuppressWarnings("unused")
		public LogItem() {
		}

		public LogItem(int date, int advertiser, int campaign, int banner, long impressions, long clicks,
				long conversions, double revenue, int userId, int errors, int affiliate, String site) {
			this.date = date;
			this.advertiser = advertiser;
			this.campaign = campaign;
			this.banner = banner;
			this.impressions = impressions;
			this.clicks = clicks;
			this.conversions = conversions;
			this.revenue = revenue;
			this.userId = userId;
			this.errors = errors;
			this.affiliate = affiliate;
			this.site = site;
		}
	}

	public static class LogItemSplitter extends SplitterLogDataConsumer<LogItem, CubeDiff> {
		private final Cube cube;

		public LogItemSplitter(Cube cube) {
			this.cube = cube;
		}

		@Override
		protected StreamDataAcceptor<LogItem> createSplitter(Context ctx) {
			return new StreamDataAcceptor<>() {
				private final StreamDataAcceptor<LogItem> dateAggregator = ctx.addOutput(
						cube.logStreamConsumer(
								LogItem.class,
								and(notEq("advertiser", EXCLUDE_ADVERTISER), notEq("campaign", EXCLUDE_CAMPAIGN), notEq("banner", EXCLUDE_BANNER))));
				private final StreamDataAcceptor<LogItem> dateAggregator2 = ctx.addOutput(
						cube.logStreamConsumer(
								LogItem.class,
								and(notEq("affiliate", EXCLUDE_AFFILIATE), notEq("site", EXCLUDE_SITE))));

				@Override
				public void accept(LogItem item) {
					if (item.advertiser != EXCLUDE_ADVERTISER && item.campaign != EXCLUDE_CAMPAIGN && item.banner != EXCLUDE_BANNER) {
						assert dateAggregator != null;
						dateAggregator.accept(item);
					} else if (item.affiliate != 0 && !EXCLUDE_SITE.equals(item.site)) {
						assert dateAggregator2 != null;
						dateAggregator2.accept(item);
					}
				}
			};
		}
	}

	@Before
	public void setUp() throws Exception {
		super.setUp();
		serverPort = getFreePort();
		Path aggregationsDir = temporaryFolder.newFolder().toPath();

		FileSystem fs = FileSystem.create(reactor, EXECUTOR, aggregationsDir);
		await(fs.start());
		IAggregationChunkStorage<Long> aggregationChunkStorage = AggregationChunkStorage.create(reactor,
				ChunkIdJsonCodec.ofLong(), AsyncSupplier.of(new RefLong(0)::inc), LZ4FrameFormat.create(), fs);
		cube = Cube.builder(reactor, EXECUTOR, CLASS_LOADER, aggregationChunkStorage)
				.withClassLoaderCache(CubeClassLoaderCache.create(CLASS_LOADER, 5))
				.initialize(cube -> DIMENSIONS_CUBE.forEach(cube::withDimension))
				.initialize(cube -> MEASURES.forEach(cube::withMeasure))
				.withRelation("campaign", "advertiser")
				.withRelation("banner", "campaign")
				.withRelation("site", "affiliate")
				.withAttribute("advertiser.name", new AdvertiserAttributeResolver(reactor))
				.withComputedMeasure("ctr", percent(measure("clicks"), measure("impressions")))
				.withComputedMeasure("uniqueUserPercent", percent(div(measure("uniqueUserIdsCount"), measure("eventCount"))))
				.withComputedMeasure("errorsPercent", percent(div(measure("errors"), measure("impressions"))))

				.withAggregation(id("advertisers")
						.withDimensions(DIMENSIONS_ADVERTISERS_AGGREGATION.keySet())
						.withMeasures(MEASURES.keySet())
						.withPredicate(and(notEq("advertiser", EXCLUDE_ADVERTISER), notEq("campaign", EXCLUDE_CAMPAIGN), notEq("banner", EXCLUDE_BANNER))))

				.withAggregation(id("affiliates")
						.withDimensions(DIMENSIONS_AFFILIATES_AGGREGATION.keySet())
						.withMeasures(MEASURES.keySet())
						.withPredicate(and(notEq("affiliate", 0), notEq("site", EXCLUDE_SITE))))

				.withAggregation(id("daily")
						.withDimensions(DIMENSIONS_DATE_AGGREGATION.keySet())
						.withMeasures(MEASURES.keySet()))
				.build();

		AsyncOTUplink<Long, LogDiff<CubeDiff>, ?> uplink = uplinkFactory.create(cube);

		LogOTState<CubeDiff> cubeDiffLogOTState = LogOTState.create(cube);
		OTStateManager<Long, LogDiff<CubeDiff>> logCubeStateManager = OTStateManager.create(reactor, LOG_OT, uplink, cubeDiffLogOTState);

		FileSystem fileSystem = FileSystem.create(reactor, EXECUTOR, temporaryFolder.newFolder().toPath());
		await(fileSystem.start());
		IMultilog<LogItem> multilog = Multilog.create(reactor,
				fileSystem,
				LZ4FrameFormat.create(),
				SerializerFactory.defaultInstance().create(CLASS_LOADER, LogItem.class),
				NAME_PARTITION_REMAINDER_SEQ);

		LogOTProcessor<LogItem, CubeDiff> logOTProcessor = LogOTProcessor.create(reactor,
				multilog,
				new LogItemSplitter(cube),
				"testlog",
				List.of("partitionA"),
				cubeDiffLogOTState);

		// checkout first (root) revision
		await(logCubeStateManager.checkout());

		List<LogItem> logItemsForAdvertisersAggregations = List.of(
				new LogItem(1, 1, 1, 1, 20, 3, 1, 0.12, 2, 2, EXCLUDE_AFFILIATE, EXCLUDE_SITE),
				new LogItem(1, 2, 2, 2, 100, 5, 0, 0.36, 10, 0, EXCLUDE_AFFILIATE, EXCLUDE_SITE),
				new LogItem(1, 3, 3, 3, 80, 5, 0, 0.60, 1, 8, EXCLUDE_AFFILIATE, EXCLUDE_SITE),
				new LogItem(2, 1, 1, 1, 15, 2, 0, 0.22, 1, 3, EXCLUDE_AFFILIATE, EXCLUDE_SITE),
				new LogItem(3, 1, 1, 1, 30, 5, 2, 0.30, 3, 4, EXCLUDE_AFFILIATE, EXCLUDE_SITE));

		List<LogItem> logItemsForAffiliatesAggregation = List.of(
				new LogItem(1, EXCLUDE_ADVERTISER, EXCLUDE_CAMPAIGN, EXCLUDE_BANNER, 10, 3, 1, 0.12, 0, 2, 1, "site3.com"),
				new LogItem(1, EXCLUDE_ADVERTISER, EXCLUDE_CAMPAIGN, EXCLUDE_BANNER, 15, 2, 0, 0.22, 0, 3, 2, "site3.com"),
				new LogItem(1, EXCLUDE_ADVERTISER, EXCLUDE_CAMPAIGN, EXCLUDE_BANNER, 30, 5, 2, 0.30, 0, 4, 2, "site3.com"),
				new LogItem(1, EXCLUDE_ADVERTISER, EXCLUDE_CAMPAIGN, EXCLUDE_BANNER, 100, 5, 0, 0.36, 0, 0, 3, "site2.com"),
				new LogItem(1, EXCLUDE_ADVERTISER, EXCLUDE_CAMPAIGN, EXCLUDE_BANNER, 80, 5, 0, 0.60, 0, 8, 4, "site1.com"),
				new LogItem(2, EXCLUDE_ADVERTISER, EXCLUDE_CAMPAIGN, EXCLUDE_BANNER, 20, 1, 12, 0.8, 0, 3, 4, "site1.com"),
				new LogItem(2, EXCLUDE_ADVERTISER, EXCLUDE_CAMPAIGN, EXCLUDE_BANNER, 30, 2, 13, 0.9, 0, 2, 4, "site1.com"),
				new LogItem(3, EXCLUDE_ADVERTISER, EXCLUDE_CAMPAIGN, EXCLUDE_BANNER, 40, 3, 2, 1.0, 0, 1, 4, "site1.com"));

		await(StreamSupplier.ofIterable(concat(logItemsForAdvertisersAggregations, logItemsForAffiliatesAggregation))
				.streamTo(StreamConsumer.ofPromise(multilog.write("partitionA"))));

		LogDiff<CubeDiff> logDiff = await(logOTProcessor.processLog());
		await(aggregationChunkStorage
				.finish(logDiff.diffs().flatMap(CubeDiff::addedChunks).map(id -> (long) id).collect(toSet())));
		logCubeStateManager.add(logDiff);
		await(logCubeStateManager.sync());

		cubeHttpServer = startHttpServer();

		IHttpClient httpClient = HttpClient.builder(reactor)
				.withNoKeepAlive()
				.build();
		cubeHttp = HttpClientCube.builder(httpClient, "http://127.0.0.1:" + serverPort)
				.withAttribute("date", LocalDate.class)
				.withAttribute("advertiser", int.class)
				.withAttribute("campaign", int.class)
				.withAttribute("banner", int.class)
				.withAttribute("affiliate", int.class)
				.withAttribute("site", String.class)
				.withAttribute("advertiser.name", String.class)
				.withMeasure("impressions", long.class)
				.withMeasure("clicks", long.class)
				.withMeasure("conversions", long.class)
				.withMeasure("revenue", double.class)
				.withMeasure("errors", long.class)
				.withMeasure("eventCount", int.class)
				.withMeasure("minRevenue", double.class)
				.withMeasure("maxRevenue", double.class)
				.withMeasure("ctr", double.class)
				.withMeasure("uniqueUserIdsCount", int.class)
				.withMeasure("uniqueUserPercent", double.class)
				.withMeasure("errorsPercent", double.class)
				.build();
	}

	private HttpServer startHttpServer() {
		HttpServer server = HttpServer.builder(reactor, ReportingServiceServlet.createRootServlet(reactor, cube))
				.withListenPort(serverPort)
				.withAcceptOnce()
				.build();
		try {
			server.listen();
		} catch (IOException e) {
			throw new AssertionError(e);
		}
		return server;
	}

	@After
	public void after() {
		if (cubeHttpServer != null) cubeHttpServer.closeFuture();
		await();
	}

	@Test
	public void testQuery() {
		CubeQuery query = CubeQuery.builder()
				.withAttributes("date")
				.withMeasures("impressions", "clicks", "ctr", "revenue")
				.withOrderingDesc("date")
				.withWhere(and(between("date", LocalDate.parse("2000-01-02"), LocalDate.parse("2000-01-03"))))
				.withReportType(DATA_WITH_TOTALS)
				.build();

		QueryResult queryResult = await(cubeHttp.query(query));

		List<Record> records = queryResult.getRecords();
		assertEquals(2, records.size());
		assertEquals(Set.of("date"), new HashSet<>(queryResult.getAttributes()));
		assertEquals(Set.of("impressions", "clicks", "ctr", "revenue"), new HashSet<>(queryResult.getMeasures()));
		assertEquals(LocalDate.parse("2000-01-03"), records.get(0).get("date"));
		assertEquals(5, (long) records.get(0).get("clicks"));
		assertEquals(65, (long) records.get(0).get("impressions"));
		assertEquals(5.0 / 65.0 * 100.0, records.get(0).get("ctr"), DELTA);
		assertEquals(LocalDate.parse("2000-01-02"), records.get(1).get("date"));
		assertEquals(33, (long) records.get(1).get("clicks"));
		assertEquals(435, (long) records.get(1).get("impressions"));
		assertEquals(33.0 / 435.0 * 100.0, records.get(1).get("ctr"), DELTA);
		assertEquals(2, queryResult.getTotalCount());
		Record totals = queryResult.getTotals();
		assertEquals(38, (long) totals.get("clicks"));
		assertEquals(500, (long) totals.get("impressions"));
		assertEquals(38.0 / 500.0 * 100.0, totals.get("ctr"), DELTA);
		assertEquals(Set.of("date"), new HashSet<>(queryResult.getSortedBy()));
	}

	@Test
	public void testDuplicateQuery() {
		CubeQuery query = CubeQuery.builder()
				.withAttributes("date")
				.withMeasures("impressions", "clicks", "ctr", "revenue")
				.withOrderingDesc("date")
				.withWhere(and(between("date", LocalDate.parse("2000-01-02"), LocalDate.parse("2000-01-03"))))
				.withReportType(DATA_WITH_TOTALS)
				.build();

		QueryResult queryResult1 = await(cubeHttp.query(query));

		List<Record> records1 = queryResult1.getRecords();
		assertEquals(2, records1.size());
		assertEquals(Set.of("date"), new HashSet<>(queryResult1.getAttributes()));
		assertEquals(Set.of("impressions", "clicks", "ctr", "revenue"), new HashSet<>(queryResult1.getMeasures()));
		assertEquals(LocalDate.parse("2000-01-03"), records1.get(0).get("date"));
		assertEquals(5, (long) records1.get(0).get("clicks"));
		assertEquals(65, (long) records1.get(0).get("impressions"));
		assertEquals(5.0 / 65.0 * 100.0, records1.get(0).get("ctr"), DELTA);
		assertEquals(LocalDate.parse("2000-01-02"), records1.get(1).get("date"));
		assertEquals(33, (long) records1.get(1).get("clicks"));
		assertEquals(435, (long) records1.get(1).get("impressions"));
		assertEquals(33.0 / 435.0 * 100.0, records1.get(1).get("ctr"), DELTA);
		assertEquals(2, queryResult1.getTotalCount());
		Record totals1 = queryResult1.getTotals();
		assertEquals(38, (long) totals1.get("clicks"));
		assertEquals(500, (long) totals1.get("impressions"));
		assertEquals(38.0 / 500.0 * 100.0, totals1.get("ctr"), DELTA);
		assertEquals(Set.of("date"), new HashSet<>(queryResult1.getSortedBy()));

		startHttpServer();
		QueryResult queryResult2 = await(cubeHttp.query(query));
		List<Record> records2 = queryResult2.getRecords();
		assertEquals(records1.size(), records2.size());
		assertEquals(queryResult1.getAttributes(), queryResult2.getAttributes());
		assertEquals(queryResult1.getMeasures(), queryResult2.getMeasures());
		assertEquals((LocalDate) records1.get(0).get("date"), records2.get(0).get("date"));
		assertEquals((long) records1.get(0).get("clicks"), (long) records2.get(0).get("clicks"));
		assertEquals((long) records1.get(0).get("impressions"), (long) records2.get(0).get("impressions"));
		assertEquals((double) records1.get(0).get("ctr"), records1.get(0).get("ctr"), DELTA);
		assertEquals((LocalDate) records1.get(1).get("date"), records2.get(1).get("date"));
		assertEquals((long) records1.get(1).get("clicks"), (long) records2.get(1).get("clicks"));
		assertEquals((long) records1.get(1).get("impressions"), (long) records2.get(1).get("impressions"));
		assertEquals((double) records1.get(1).get("ctr"), records2.get(1).get("ctr"), DELTA);
		assertEquals(queryResult1.getTotalCount(), queryResult2.getTotalCount());
		Record totals2 = queryResult2.getTotals();
		assertEquals((long) totals1.get("clicks"), (long) totals2.get("clicks"));
		assertEquals((long) totals1.get("impressions"), (long) totals2.get("impressions"));
		assertEquals((double) totals1.get("ctr"), totals2.get("ctr"), DELTA);
		assertEquals(queryResult1.getSortedBy(), queryResult2.getSortedBy());
	}

	@Test
	public void testQueryWithPredicateLe() {
		CubeQuery query = CubeQuery.builder()
				.withAttributes("date")
				.withMeasures("impressions", "clicks", "ctr", "revenue")
				.withOrderingDesc("date")
				.withWhere(and(le("date", LocalDate.parse("2000-01-03"))))
				.withReportType(DATA_WITH_TOTALS)
				.build();

		QueryResult queryResult = await(cubeHttp.query(query));

		List<Record> records = queryResult.getRecords();
		assertEquals(2, records.size());
		assertEquals(Set.of("date"), new HashSet<>(queryResult.getAttributes()));
		assertEquals(Set.of("impressions", "clicks", "ctr", "revenue"), new HashSet<>(queryResult.getMeasures()));
		assertEquals(LocalDate.parse("2000-01-03"), records.get(0).get("date"));
		assertEquals(5, (long) records.get(0).get("clicks"));
		assertEquals(65, (long) records.get(0).get("impressions"));
		assertEquals(5.0 / 65.0 * 100.0, records.get(0).get("ctr"), DELTA);
		assertEquals(LocalDate.parse("2000-01-02"), records.get(1).get("date"));
		assertEquals(33, (long) records.get(1).get("clicks"));
		assertEquals(435, (long) records.get(1).get("impressions"));
		assertEquals(33.0 / 435.0 * 100.0, records.get(1).get("ctr"), DELTA);
		assertEquals(2, queryResult.getTotalCount());
		Record totals = queryResult.getTotals();
		assertEquals(38, (long) totals.get("clicks"));
		assertEquals(500, (long) totals.get("impressions"));
		assertEquals(38.0 / 500.0 * 100.0, totals.get("ctr"), DELTA);
		assertEquals(Set.of("date"), new HashSet<>(queryResult.getSortedBy()));
	}

	@Test
	public void testQueryAffectingAdvertisersAggregation() {
		CubeQuery query = CubeQuery.builder()
				.withAttributes("date")
				.withMeasures("impressions", "clicks", "ctr", "revenue")
				.withWhere(and(
						eq("banner", 1),
						between("date", LocalDate.parse("2000-01-02"), LocalDate.parse("2000-01-03")),
						and(notEq("advertiser", EXCLUDE_ADVERTISER), notEq("banner", EXCLUDE_BANNER), notEq("campaign", EXCLUDE_CAMPAIGN))))
				.withOrderings(asc("ctr"))
				.withReportType(DATA_WITH_TOTALS)
				.build();

		QueryResult queryResult = await(cubeHttp.query(query));

		List<Record> records = queryResult.getRecords();
		assertEquals(2, records.size());
		assertEquals(Set.of("date"), new HashSet<>(queryResult.getAttributes()));
		assertEquals(Set.of("impressions", "clicks", "ctr", "revenue"), new HashSet<>(queryResult.getMeasures()));
		assertEquals(LocalDate.parse("2000-01-03"), records.get(0).get("date"));
		assertEquals(2, (long) records.get(0).get("clicks"));
		assertEquals(15, (long) records.get(0).get("impressions"));
		assertEquals(2.0 / 15.0 * 100.0, records.get(0).get("ctr"), DELTA);
		assertEquals(LocalDate.parse("2000-01-02"), records.get(1).get("date"));
		assertEquals(3, (long) records.get(1).get("clicks"));
		assertEquals(20, (long) records.get(1).get("impressions"));
		assertEquals(3.0 / 20.0 * 100.0, records.get(1).get("ctr"), DELTA);
		assertEquals(2, queryResult.getTotalCount());
		Record totals = queryResult.getTotals();
		assertEquals(35, (long) totals.get("impressions"));
		assertEquals(5, (long) totals.get("clicks"));
		assertEquals(5.0 / 35.0 * 100.0, totals.get("ctr"), DELTA);
		assertEquals(Set.of("ctr"), new HashSet<>(queryResult.getSortedBy()));
	}

	@Test
	public void testImpressionsByDate() {
		CubeQuery query = CubeQuery.builder()
				.withAttributes("date")
				.withMeasures("impressions")
				.withReportType(DATA)
				.build();

		QueryResult queryResult = await(cubeHttp.query(query));

		List<Record> records = queryResult.getRecords();
		assertEquals(3, records.size());

		Record r1 = records.get(0);
		assertEquals(LocalDate.parse("2000-01-02"), r1.get("date"));
		assertEquals(435, (long) r1.get("impressions"));

		Record r2 = records.get(1);
		assertEquals(LocalDate.parse("2000-01-03"), r2.get("date"));
		assertEquals(65, (long) r2.get("impressions"));

		Record r3 = records.get(2);
		assertEquals(LocalDate.parse("2000-01-04"), r3.get("date"));
		assertEquals(70, (long) r3.get("impressions"));
	}

	@Test
	public void testQueryWithNullAttributes() {
		CubeQuery query = CubeQuery.builder()
				.withAttributes("date", "advertiser.name", "advertiser")
				.withMeasures("impressions")
				.withOrderings(asc("date"), asc("advertiser.name"))
				.withWhere(and(
						between("date", LocalDate.parse("2000-01-01"), LocalDate.parse("2000-01-04")),
						and(notEq("advertiser", EXCLUDE_ADVERTISER), notEq("campaign", EXCLUDE_CAMPAIGN), notEq("banner", EXCLUDE_BANNER))))
				.withHaving(and(
						or(eq("advertiser.name", null), eq("advertiser.name", "first")),
						between("date", LocalDate.parse("2000-01-01"), LocalDate.parse("2000-01-03"))))
				.withReportType(DATA_WITH_TOTALS)
				.build();

		QueryResult queryResult = await(cubeHttp.query(query));

		List<Record> records = queryResult.getRecords();
		assertEquals(3, records.size());
		assertEquals(Set.of("date", "advertiser", "advertiser.name"), new HashSet<>(queryResult.getAttributes()));
		assertEquals(Set.of("impressions"), new HashSet<>(queryResult.getMeasures()));

		assertEquals(LocalDate.parse("2000-01-02"), records.get(0).get("date"));
		assertEquals(2, (int) records.get(0).get("advertiser"));
		assertNull(records.get(0).get("advertiser.name"));
		assertEquals(100, (long) records.get(0).get("impressions"));

		assertEquals(LocalDate.parse("2000-01-02"), records.get(1).get("date"));
		assertEquals(1, (int) records.get(1).get("advertiser"));
		assertEquals("first", records.get(1).get("advertiser.name"));
		assertEquals(20, (long) records.get(1).get("impressions"));

		assertEquals(LocalDate.parse("2000-01-03"), records.get(2).get("date"));
		assertEquals(1, (int) records.get(2).get("advertiser"));
		assertEquals("first", records.get(2).get("advertiser.name"));
		assertEquals(15, (long) records.get(2).get("impressions"));

		Record totals = queryResult.getTotals();
		// totals evaluated before applying having predicate
		assertEquals(245, (long) totals.get("impressions"));
		assertEquals(Set.of("date", "advertiser.name"), new HashSet<>(queryResult.getSortedBy()));
	}

	@Test
	public void testQueryWithNullAttributeAndBetweenPredicate() {
		CubeQuery query = CubeQuery.builder()
				.withAttributes("advertiser.name")
				.withMeasures("impressions")
				.withWhere(and(notEq("advertiser", EXCLUDE_ADVERTISER), notEq("campaign", EXCLUDE_CAMPAIGN), notEq("banner", EXCLUDE_BANNER)))
				.withHaving(or(between("advertiser.name", "a", "z"), eq("advertiser.name", null)))
				.withReportType(DATA)
				.build();

		QueryResult queryResult = await(cubeHttp.query(query));

		List<Record> records = queryResult.getRecords();
		assertEquals(3, records.size());
		assertEquals("first", records.get(0).get("advertiser.name"));
		assertNull(records.get(1).get("advertiser.name"));
		assertEquals("third", records.get(2).get("advertiser.name"));
	}

	@Test
	public void testFilterAttributes() {
		CubeQuery query = CubeQuery.builder()
				.withAttributes("date", "advertiser.name")
				.withMeasures("impressions")
				.withWhere(and(eq("advertiser", 2), notEq("advertiser", EXCLUDE_ADVERTISER), notEq("campaign", EXCLUDE_CAMPAIGN), notEq("banner", EXCLUDE_BANNER)))
				.withOrderings(asc("advertiser.name"))
				.withHaving(eq("advertiser.name", null))
				.build();

		QueryResult queryResult = await(cubeHttp.query(query));

		Map<String, Object> filterAttributes = queryResult.getFilterAttributes();
		assertEquals(1, filterAttributes.size());
		assertTrue(filterAttributes.containsKey("advertiser.name"));
		assertNull(filterAttributes.get("advertiser.name"));
	}

	@Test
	public void testRecordsWithFullySpecifiedAttributes() {
		CubeQuery query = CubeQuery.builder()
				.withAttributes("date", "advertiser.name")
				.withMeasures("impressions")
				.withWhere(and(eq("advertiser", 1), notEq("campaign", EXCLUDE_CAMPAIGN), notEq("banner", EXCLUDE_BANNER)))
				.build();

		QueryResult queryResult = await(cubeHttp.query(query));

		assertEquals(3, queryResult.getRecords().size());
		assertEquals("first", queryResult.getRecords().get(0).get("advertiser.name"));
		assertEquals("first", queryResult.getRecords().get(1).get("advertiser.name"));
		assertEquals("first", queryResult.getRecords().get(2).get("advertiser.name"));
	}

	@Test
	public void testSearchAndFieldsParameter() {
		CubeQuery query = CubeQuery.builder()
				.withAttributes("advertiser.name")
				.withMeasures("clicks")
				.withWhere(and(not(eq("advertiser", EXCLUDE_ADVERTISER)), notEq("campaign", EXCLUDE_CAMPAIGN), notEq("banner", EXCLUDE_BANNER)))
				.withHaving(or(regexp("advertiser.name", ".*s.*"), eq("advertiser.name", null)))
				.withReportType(DATA)
				.build();

		QueryResult queryResult = await(cubeHttp.query(query));

		List<Record> records = queryResult.getRecords();
		assertEquals(2, records.size());
		assertEquals(List.of("advertiser.name", "clicks"), records.get(0).getScheme().getFields());
		assertEquals(List.of("advertiser.name"), queryResult.getAttributes());
		assertEquals(List.of("clicks"), queryResult.getMeasures());
		assertEquals("first", records.get(0).get("advertiser.name"));
		assertNull(records.get(1).get("advertiser.name"));
	}

	@Test
	public void testCustomMeasures() {
		CubeQuery query = CubeQuery.builder()
				.withAttributes("date")
				.withMeasures("eventCount", "minRevenue", "maxRevenue", "uniqueUserIdsCount", "uniqueUserPercent", "clicks")
				.withOrderings(asc("date"), asc("uniqueUserIdsCount"))
				.withReportType(DATA_WITH_TOTALS)
				.build();

		QueryResult queryResult = await(cubeHttp.query(query));

		List<Record> records = queryResult.getRecords();
		assertEquals(Set.of("eventCount", "minRevenue", "maxRevenue", "uniqueUserIdsCount", "uniqueUserPercent", "clicks"),
				new HashSet<>(queryResult.getMeasures()));
		assertEquals(3, records.size());

		Record r1 = records.get(0);
		assertEquals(LocalDate.parse("2000-01-02"), r1.get("date"));
		assertEquals(0.12, r1.get("minRevenue"), DELTA);
		assertEquals(0.6, r1.get("maxRevenue"), DELTA);
		assertEquals(8, (int) r1.get("eventCount"));
		assertEquals(4, (int) r1.get("uniqueUserIdsCount"));
		assertEquals(50.0, r1.get("uniqueUserPercent"), DELTA);

		Record r2 = records.get(1);
		assertEquals(LocalDate.parse("2000-01-03"), r2.get("date"));
		assertEquals(0.22, r2.get("minRevenue"), DELTA);
		assertEquals(0.9, r2.get("maxRevenue"), DELTA);
		assertEquals(3, (int) r2.get("eventCount"));
		assertEquals(2, (int) r2.get("uniqueUserIdsCount"));
		assertEquals(2.0 / 3.0 * 100, r2.get("uniqueUserPercent"), DELTA);

		Record r3 = records.get(2);
		assertEquals(LocalDate.parse("2000-01-04"), r3.get("date"));
		assertEquals(0.30, r3.get("minRevenue"), DELTA);
		assertEquals(1.0, r3.get("maxRevenue"), DELTA);
		assertEquals(2, (int) r3.get("eventCount"));
		assertEquals(2, (int) r3.get("uniqueUserIdsCount"));
		assertEquals(100.0, r3.get("uniqueUserPercent"), DELTA);

		Record totals = queryResult.getTotals();
		assertEquals(0.12, totals.get("minRevenue"), DELTA);
		assertEquals(1.0, totals.get("maxRevenue"), DELTA);
		assertEquals(13, (int) totals.get("eventCount"));
		assertEquals(5, (int) totals.get("uniqueUserIdsCount"));
		assertEquals(5 / 13.0 * 100, totals.get("uniqueUserPercent"), DELTA);
		assertEquals(46, (long) totals.get("clicks"));
	}

	@Test
	public void testMetadataOnlyQuery() {
		String[] attributes = {"date", "advertiser", "advertiser.name"};
		CubeQuery onlyMetaQuery = CubeQuery.builder()
				.withAttributes(attributes)
				.withWhere(and(
						notEq("advertiser", EXCLUDE_ADVERTISER),
						notEq("banner", EXCLUDE_BANNER),
						notEq("campaign", EXCLUDE_CAMPAIGN)))
				.withMeasures("clicks", "ctr", "conversions")
				.withOrderingDesc("date")
				.withOrderingAsc("advertiser.name")
				.withReportType(ReportType.METADATA)
				.build();

		QueryResult metadata = await(cubeHttp.query(onlyMetaQuery));

		assertEquals(6, metadata.getRecordScheme().getFields().size());
		assertEquals(0, metadata.getTotalCount());
		assertTrue(metadata.getRecords().isEmpty());
		assertTrue(metadata.getFilterAttributes().isEmpty());
	}

	@Test
	public void testQueryWithInPredicate() {
		CubeQuery queryWithPredicateIn = CubeQuery.builder()
				.withAttributes("advertiser")
				.withWhere(and(
						in("advertiser", List.of(1, 2)),
						notEq("advertiser", EXCLUDE_ADVERTISER),
						notEq("banner", EXCLUDE_BANNER),
						notEq("campaign", EXCLUDE_CAMPAIGN)))
				.withMeasures("clicks", "ctr", "conversions")
				.withReportType(DATA_WITH_TOTALS)
				.withHaving(in("advertiser", List.of(1, 2)))
				.build();

		QueryResult in = await(cubeHttp.query(queryWithPredicateIn));

		List<String> expectedRecordFields = List.of("advertiser", "clicks", "ctr", "conversions");
		assertEquals(expectedRecordFields.size(), in.getRecordScheme().getFields().size());
		assertEquals(2, in.getTotalCount());

		assertEquals(1, in.getRecords().get(0).getInt("advertiser"));
		assertEquals(2, in.getRecords().get(1).getInt("advertiser"));
	}

	@Test
	public void testMetaOnlyQueryHasEmptyMeasuresWhenNoAggregationsFound() {
		CubeQuery queryAffectingNonCompatibleAggregations = CubeQuery.builder()
				.withAttributes("date", "advertiser", "affiliate")
				.withMeasures("errors", "errorsPercent")
				.withReportType(ReportType.METADATA)
				.build();

		QueryResult metadata = await(cubeHttp.query(queryAffectingNonCompatibleAggregations));
		assertEquals(0, metadata.getMeasures().size());
	}

	@Test
	public void testMetaOnlyQueryResultHasCorrectMeasuresWhenSomeAggregationsAreIncompatible() {
		CubeQuery queryAffectingNonCompatibleAggregations = CubeQuery.builder()
				.withAttributes("date", "advertiser")
				.withMeasures("impressions", "incompatible_measure", "clicks")
				.withWhere(and(
						notEq("advertiser", EXCLUDE_ADVERTISER),
						notEq("campaign", EXCLUDE_CAMPAIGN),
						notEq("banner", EXCLUDE_BANNER)))
				.withReportType(ReportType.METADATA)
				.build();

		QueryResult metadata = await(cubeHttp.query(queryAffectingNonCompatibleAggregations));
		List<String> expectedMeasures = List.of("impressions", "clicks");
		assertEquals(expectedMeasures, metadata.getMeasures());
	}

	@Test
	public void testDataCorrectlyLoadedIntoAggregations() {
		Aggregation daily = cube.getAggregation("daily");
		assert daily != null;
		int dailyAggregationItemsCount = getAggregationItemsCount(daily);
		assertEquals(6, dailyAggregationItemsCount);

		Aggregation advertisers = cube.getAggregation("advertisers");
		assert advertisers != null;
		int advertisersAggregationItemsCount = getAggregationItemsCount(advertisers);
		assertEquals(5, advertisersAggregationItemsCount);

		Aggregation affiliates = cube.getAggregation("affiliates");
		assert affiliates != null;
		int affiliatesAggregationItemsCount = getAggregationItemsCount(affiliates);
		assertEquals(6, affiliatesAggregationItemsCount);
	}

	@Test
	public void testAdvertisersAggregationTotals() {
		CubeQuery queryAdvertisers = CubeQuery.builder()
				.withAttributes("date", "advertiser")
				.withMeasures(List.of("clicks", "impressions", "revenue", "errors"))
				.withWhere(and(notEq("advertiser", EXCLUDE_ADVERTISER), notEq("campaign", EXCLUDE_CAMPAIGN), notEq("banner", EXCLUDE_BANNER),
						between("date", LocalDate.parse("2000-01-02"), LocalDate.parse("2000-01-02"))))
				.withReportType(DATA_WITH_TOTALS)
				.build();

		QueryResult resultByAdvertisers = await(cubeHttp.query(queryAdvertisers));

		Record advertisersTotals = resultByAdvertisers.getTotals();
		long advertisersImpressions = advertisersTotals.get("impressions");
		long advertisersClicks = advertisersTotals.get("clicks");
		double advertisersRevenue = advertisersTotals.get("revenue");
		long advertisersErrors = advertisersTotals.get("errors");
		assertEquals(200, advertisersImpressions);
		assertEquals(13, advertisersClicks);
		assertEquals(1.08, advertisersRevenue, Double.MIN_VALUE);
		assertEquals(10, advertisersErrors);
	}

	@Test
	public void testAffiliatesAggregationTotals() {
		CubeQuery queryAffiliates = CubeQuery.builder()
				.withAttributes("date", "affiliate")
				.withMeasures(List.of("clicks", "impressions", "revenue", "errors"))
				.withWhere(and(notEq("affiliate", 0), notEq("site", EXCLUDE_SITE),
						between("date", LocalDate.parse("2000-01-02"), LocalDate.parse("2000-01-02"))))
				.withReportType(DATA_WITH_TOTALS)
				.build();

		QueryResult resultByAffiliates = await(cubeHttp.query(queryAffiliates));

		Record affiliatesTotals = resultByAffiliates.getTotals();
		long affiliatesImpressions = affiliatesTotals.get("impressions");
		long affiliatesClicks = affiliatesTotals.get("clicks");
		double affiliatesRevenue = affiliatesTotals.get("revenue");
		long affiliatesErrors = affiliatesTotals.get("errors");
		assertEquals(235, affiliatesImpressions);
		assertEquals(20, affiliatesClicks);
		assertEquals(1.60, affiliatesRevenue, Double.MIN_VALUE);
		assertEquals(17, affiliatesErrors);
	}

	@Test
	public void testDailyAggregationTotals() {
		CubeQuery queryDate = CubeQuery.builder()
				.withAttributes("date")
				.withMeasures(List.of("clicks", "impressions", "revenue", "errors"))
				.withWhere(between("date", LocalDate.parse("2000-01-02"), LocalDate.parse("2000-01-02")))
				.withReportType(DATA_WITH_TOTALS)
				.build();

		QueryResult resultByDate = await(cubeHttp.query(queryDate));

		Record dailyTotals = resultByDate.getTotals();
		long dailyImpressions = dailyTotals.get("impressions");
		long dailyClicks = dailyTotals.get("clicks");
		double dailyRevenue = dailyTotals.get("revenue");
		long dailyErrors = dailyTotals.get("errors");
		assertEquals(435, dailyImpressions);
		assertEquals(33, dailyClicks);
		assertEquals(2.68, dailyRevenue, DELTA);
		assertEquals(27, dailyErrors);
	}

	@Test
	public void testResultContainsTotals_whenDataWithTotalsRequested() {
		List<String> measures = List.of("clicks", "impressions", "revenue", "errors");
		List<String> requestMeasures = new ArrayList<>(measures);
		requestMeasures.add(3, "nonexistentMeasure");
		List<String> dateDimension = List.of("date");
		CubeQuery queryDate = CubeQuery.builder()
				.withAttributes(dateDimension)
				.withMeasures(requestMeasures)
				.withWhere(between("date", LocalDate.parse("2000-01-02"), LocalDate.parse("2000-01-02")))
				.withReportType(DATA_WITH_TOTALS)
				.build();

		QueryResult resultByDate = await(cubeHttp.query(queryDate));

		assertEquals(dateDimension, resultByDate.getAttributes());
		assertEquals(measures, resultByDate.getMeasures());

		Record dailyTotals = resultByDate.getTotals();
		long dailyImpressions = dailyTotals.get("impressions");
		long dailyClicks = dailyTotals.get("clicks");
		double dailyRevenue = dailyTotals.get("revenue");
		long dailyErrors = dailyTotals.get("errors");
		assertEquals(435, dailyImpressions);
		assertEquals(33, dailyClicks);
		assertEquals(2.68, dailyRevenue, DELTA);
		assertEquals(27, dailyErrors);
	}

	@Test
	public void testResultContainsTotalsAndMetadata_whenTotalsAndMetadataRequested() {
		List<String> measures = List.of("clicks", "impressions", "revenue", "errors");
		List<String> requestMeasures = new ArrayList<>(measures);
		requestMeasures.add("unexpected");
		CubeQuery queryDate = CubeQuery.builder()
				.withAttributes("date")
				.withMeasures(requestMeasures)
				.withWhere(between("date", LocalDate.parse("2000-01-02"), LocalDate.parse("2000-01-02")))
				.withReportType(DATA_WITH_TOTALS)
				.build();

		QueryResult resultByDate = await(cubeHttp.query(queryDate));

		assertEquals(1, resultByDate.getAttributes().size());
		assertEquals("date", resultByDate.getAttributes().get(0));
		assertEquals(resultByDate.getMeasures(), measures);
		Record dailyTotals = resultByDate.getTotals();
		long dailyImpressions = dailyTotals.get("impressions");
		long dailyClicks = dailyTotals.get("clicks");
		double dailyRevenue = dailyTotals.get("revenue");
		long dailyErrors = dailyTotals.get("errors");
		assertEquals(435, dailyImpressions);
		assertEquals(33, dailyClicks);
		assertEquals(2.68, dailyRevenue, DELTA);
		assertEquals(27, dailyErrors);
	}

	private static int getAggregationItemsCount(Aggregation aggregation) {
		int count = 0;
		for (Map.Entry<Object, AggregationChunk> chunk : aggregation.getState().getChunks().entrySet()) {
			count += chunk.getValue().getCount();
		}
		return count;
	}
}
