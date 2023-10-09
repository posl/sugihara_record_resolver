package io.activej.datastream.processor;

import io.activej.datastream.StreamConsumer;
import io.activej.datastream.StreamSupplier;
import io.activej.promise.Promise;
import io.activej.test.ExpectedException;

import java.util.List;

import static io.activej.common.Checks.checkNotNull;

public final class FailingStreamSorterStorageStub<T> implements StreamSorterStorage<T> {
	static final Exception STORAGE_EXCEPTION = new ExpectedException("failing storage");

	private StreamSorterStorage<T> storage;

	boolean failNewPartition;
	boolean failWrite;
	boolean failRead;
	boolean failCleanup;

	private FailingStreamSorterStorageStub(StreamSorterStorage<T> storage) {
		this.storage = storage;
	}

	public static <T> FailingStreamSorterStorageStub<T> create(StreamSorterStorage<T> storage) {
		return new FailingStreamSorterStorageStub<>(storage);
	}

	public static <T> FailingStreamSorterStorageStub<T> create() {
		return new FailingStreamSorterStorageStub<>(null);
	}

	public FailingStreamSorterStorageStub<T> withFailNewPartition(){
		this.failNewPartition = true;
		return this;
	}

	public FailingStreamSorterStorageStub<T> withFailWrite(){
		this.failWrite = true;
		return this;
	}

	public FailingStreamSorterStorageStub<T> withFailRead(){
		this.failRead = true;
		return this;
	}

	public FailingStreamSorterStorageStub<T> withFailCleanup(){
		this.failCleanup = true;
		return this;
	}

	public void setStorage(StreamSorterStorage<T> storage){
		this.storage = storage;
	}

	@Override
	public Promise<Integer> newPartitionId() {
		checkNotNull(storage);
		return failNewPartition ? Promise.ofException(STORAGE_EXCEPTION) : storage.newPartitionId();
	}

	@Override
	public Promise<StreamConsumer<T>> write(int partition) {
		checkNotNull(storage);
		return failWrite ? Promise.ofException(STORAGE_EXCEPTION) : storage.write(partition);
	}

	@Override
	public Promise<StreamSupplier<T>> read(int partition) {
		checkNotNull(storage);
		return failRead ? Promise.ofException(STORAGE_EXCEPTION) : storage.read(partition);
	}

	@Override
	public Promise<Void> cleanup(List<Integer> partitionsToDelete) {
		checkNotNull(storage);
		return failCleanup ? Promise.ofException(STORAGE_EXCEPTION) : storage.cleanup(partitionsToDelete);
	}
}
