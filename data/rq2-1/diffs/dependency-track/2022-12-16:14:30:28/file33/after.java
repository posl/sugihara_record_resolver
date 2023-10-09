/*
 * This file is part of Dependency-Track.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright (c) Steve Springett. All Rights Reserved.
 */
package org.dependencytrack.persistence;

import alpine.event.framework.Event;
import alpine.persistence.PaginatedResult;
import alpine.resources.AlpineRequest;
import org.apache.commons.lang3.StringUtils;
import org.dependencytrack.event.IndexEvent;
import org.dependencytrack.model.AffectedVersionAttribution;
import org.dependencytrack.model.Analysis;
import org.dependencytrack.model.Component;
import org.dependencytrack.model.FindingAttribution;
import org.dependencytrack.model.Project;
import org.dependencytrack.model.Vulnerability;
import org.dependencytrack.model.VulnerabilityAlias;
import org.dependencytrack.model.VulnerableSoftware;
import org.dependencytrack.tasks.scanners.AnalyzerIdentity;

import javax.jdo.PersistenceManager;
import javax.jdo.Query;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

final class VulnerabilityQueryManager extends QueryManager implements IQueryManager {

    /**
     * Constructs a new QueryManager.
     * @param pm a PersistenceManager object
     */
    VulnerabilityQueryManager(final PersistenceManager pm) {
        super(pm);
    }

    /**
     * Constructs a new QueryManager.
     * @param pm a PersistenceManager object
     * @param request an AlpineRequest object
     */
    VulnerabilityQueryManager(final PersistenceManager pm, final AlpineRequest request) {
        super(pm, request);
    }

    /**
     * Creates a new Vulnerability.
     * @param vulnerability the vulnerability to persist
     * @param commitIndex specifies if the search index should be committed (an expensive operation)
     * @return a new vulnerability object
     */
    public Vulnerability createVulnerability(Vulnerability vulnerability, boolean commitIndex) {
        final Vulnerability result = persist(vulnerability);
        Event.dispatch(new IndexEvent(IndexEvent.Action.CREATE, pm.detachCopy(result)));
        commitSearchIndex(commitIndex, Vulnerability.class);
        return result;
    }

    /**
     * Updates a vulnerability.
     * @param transientVulnerability the vulnerability to update
     * @param commitIndex specifies if the search index should be committed (an expensive operation)
     * @return a Vulnerability object
     */
    public Vulnerability updateVulnerability(Vulnerability transientVulnerability, boolean commitIndex) {
        final Vulnerability vulnerability;
        if (transientVulnerability.getId() > 0) {
            vulnerability = getObjectById(Vulnerability.class, transientVulnerability.getId());
        } else {
            vulnerability = getVulnerabilityByVulnId(transientVulnerability.getSource(), transientVulnerability.getVulnId());
        }
        if (vulnerability != null) {
            vulnerability.setCreated(transientVulnerability.getCreated());
            vulnerability.setPublished(transientVulnerability.getPublished());
            vulnerability.setUpdated(transientVulnerability.getUpdated());
            vulnerability.setVulnId(transientVulnerability.getVulnId());
            vulnerability.setSource(transientVulnerability.getSource());
            vulnerability.setCredits(transientVulnerability.getCredits());
            vulnerability.setVulnerableVersions(transientVulnerability.getVulnerableVersions());
            vulnerability.setPatchedVersions(transientVulnerability.getPatchedVersions());
            vulnerability.setDescription(transientVulnerability.getDescription());
            vulnerability.setDetail(transientVulnerability.getDetail());
            vulnerability.setTitle(transientVulnerability.getTitle());
            vulnerability.setSubTitle(transientVulnerability.getSubTitle());
            vulnerability.setReferences(transientVulnerability.getReferences());
            vulnerability.setRecommendation(transientVulnerability.getRecommendation());
            vulnerability.setSeverity(transientVulnerability.getSeverity());
            vulnerability.setCvssV2Vector(transientVulnerability.getCvssV2Vector());
            vulnerability.setCvssV2BaseScore(transientVulnerability.getCvssV2BaseScore());
            vulnerability.setCvssV2ImpactSubScore(transientVulnerability.getCvssV2ImpactSubScore());
            vulnerability.setCvssV2ExploitabilitySubScore(transientVulnerability.getCvssV2ExploitabilitySubScore());
            vulnerability.setCvssV3Vector(transientVulnerability.getCvssV3Vector());
            vulnerability.setCvssV3BaseScore(transientVulnerability.getCvssV3BaseScore());
            vulnerability.setCvssV3ImpactSubScore(transientVulnerability.getCvssV3ImpactSubScore());
            vulnerability.setCvssV3ExploitabilitySubScore(transientVulnerability.getCvssV3ExploitabilitySubScore());
            vulnerability.setOwaspRRLikelihoodScore(transientVulnerability.getOwaspRRLikelihoodScore());
            vulnerability.setOwaspRRBusinessImpactScore(transientVulnerability.getOwaspRRBusinessImpactScore());
            vulnerability.setOwaspRRTechnicalImpactScore(transientVulnerability.getOwaspRRTechnicalImpactScore());
            vulnerability.setOwaspRRVector(transientVulnerability.getOwaspRRVector());
            vulnerability.setCwes(transientVulnerability.getCwes());
            if (transientVulnerability.getVulnerableSoftware() != null) {
                vulnerability.setVulnerableSoftware(transientVulnerability.getVulnerableSoftware());
            }
            final Vulnerability result = persist(vulnerability);
            Event.dispatch(new IndexEvent(IndexEvent.Action.UPDATE, pm.detachCopy(result)));
            commitSearchIndex(commitIndex, Vulnerability.class);
            return result;
        }
        return null;
    }

    /**
     * Synchronizes a vulnerability. Method first checkes to see if the vulnerability already
     * exists and if so, updates the vulnerability. If the vulnerability does not already exist,
     * this method will create a new vulnerability.
     * @param vulnerability the vulnerability to synchronize
     * @param commitIndex specifies if the search index should be committed (an expensive operation)
     * @return a Vulnerability object
     */
    public Vulnerability synchronizeVulnerability(Vulnerability vulnerability, boolean commitIndex) {
        Vulnerability result = updateVulnerability(vulnerability, commitIndex);
        if (result == null) {
            result = createVulnerability(vulnerability, commitIndex);
        }
        return result;
    }

    /**
     * Returns a vulnerability by it's name (i.e. CVE-2017-0001) and source.
     * @param source the source of the vulnerability
     * @param vulnId the name of the vulnerability
     * @return the matching Vulnerability object, or null if not found
     */
    public Vulnerability getVulnerabilityByVulnId(String source, String vulnId, boolean includeVulnerableSoftware) {
        final Query<Vulnerability> query = pm.newQuery(Vulnerability.class, "source == :source && vulnId == :vulnId");
        query.getFetchPlan().addGroup(Vulnerability.FetchGroup.COMPONENTS.name());
        if (includeVulnerableSoftware) {
            query.getFetchPlan().addGroup(Vulnerability.FetchGroup.VULNERABLE_SOFTWARE.name());
        }
        query.setRange(0, 1);
        final Vulnerability vulnerability = singleResult(query.execute(source, vulnId));
        if (vulnerability != null) {
            vulnerability.setAliases(getVulnerabilityAliases(vulnerability));
        }
        return vulnerability;
    }

    /**
     * Returns a vulnerability by it's name (i.e. CVE-2017-0001) and source.
     * @param source the source of the vulnerability
     * @param vulnId the name of the vulnerability
     * @return the matching Vulnerability object, or null if not found
     */
    public Vulnerability getVulnerabilityByVulnId(Vulnerability.Source source, String vulnId, boolean includeVulnerableSoftware) {
        return getVulnerabilityByVulnId(source.name(), vulnId, includeVulnerableSoftware);
    }

    /**
     * Returns vulnerabilities for the specified npm module
     * @param module the NPM module to query on
     * @return a list of Vulnerability objects
     */
    @Deprecated
    @SuppressWarnings("unchecked")
    //todo: determine if this is needed and delete
    public List<Vulnerability> getVulnerabilitiesForNpmModule(String module) {
        final Query<Vulnerability> query = pm.newQuery(Vulnerability.class, "source == :source && subtitle == :module");
        query.getFetchPlan().addGroup(Vulnerability.FetchGroup.COMPONENTS.name());
        return (List<Vulnerability>) query.execute(Vulnerability.Source.NPM.name(), module);
    }

    /**
     * Adds a vulnerability to a component.
     * @param vulnerability the vulnerability to add
     * @param component the component affected by the vulnerability
     * @param analyzerIdentity the identify of the analyzer
     */
    public void addVulnerability(Vulnerability vulnerability, Component component, AnalyzerIdentity analyzerIdentity) {
        this.addVulnerability(vulnerability, component, analyzerIdentity, null, null);
    }

    /**
     * Adds a vulnerability to a component.
     * @param vulnerability the vulnerability to add
     * @param component the component affected by the vulnerability
     * @param analyzerIdentity the identify of the analyzer
     * @param alternateIdentifier the optional identifier if the analyzer refers to the vulnerability by an alternative identifier
     * @param referenceUrl the optional URL that references the occurrence of the vulnerability if uniquely identified
     */
    public void addVulnerability(Vulnerability vulnerability, Component component, AnalyzerIdentity analyzerIdentity,
                                 String alternateIdentifier, String referenceUrl) {
        if (!contains(vulnerability, component)) {
            component.addVulnerability(vulnerability);
            component = persist(component);
            persist(new FindingAttribution(component, vulnerability, analyzerIdentity, alternateIdentifier, referenceUrl));
        }
    }

    /**
     * Removes a vulnerability from a component.
     * @param vulnerability the vulnerabillity to remove
     * @param component the component unaffected by the vulnerabiity
     */
    public void removeVulnerability(Vulnerability vulnerability, Component component) {
        if (contains(vulnerability, component)) {
            pm.currentTransaction().begin();
            component.removeVulnerability(vulnerability);
            pm.currentTransaction().commit();
        }
        final FindingAttribution fa = getFindingAttribution(vulnerability, component);
        if (fa != null) {
            delete(fa);
        }
    }

    /**
     * Returns a FindingAttribution object form a given vulnerability and component.
     * @param vulnerability the vulnerabillity of the finding attribution
     * @param component the component of the finding attribution
     * @return a FindingAttribution object
     */
    public FindingAttribution getFindingAttribution(Vulnerability vulnerability, Component component) {
        final Query<FindingAttribution> query = pm.newQuery(FindingAttribution.class, "vulnerability == :vulnerability && component == :component");
        query.setRange(0, 1);
        return singleResult(query.execute(vulnerability, component));
    }

    /**
     * Deleted all FindingAttributions associated for the specified Component.
     * @param component the Component to delete FindingAttributions for
     */
    void deleteFindingAttributions(Component component) {
        final Query<FindingAttribution> query = pm.newQuery(FindingAttribution.class, "component == :component");
        query.deletePersistentAll(component);
    }

    /**
     * Deleted all FindingAttributions associated for the specified Component.
     * @param project the Component to delete FindingAttributions for
     */
    void deleteFindingAttributions(Project project) {
        final Query<FindingAttribution> query = pm.newQuery(FindingAttribution.class, "project == :project");
        query.deletePersistentAll(project);
    }

    /**
     * Determines if a Component is affected by a specific Vulnerability by checking
     * {@link Vulnerability#getSource()} and {@link Vulnerability#getVulnId()}.
     * @param vulnerability The vulnerability to check if associated with component
     * @param component The component to check against
     * @return true if vulnerability is associated with the component, false if not
     */
    public boolean contains(Vulnerability vulnerability, Component component) {
        vulnerability = getObjectById(Vulnerability.class, vulnerability.getId());
        component = getObjectById(Component.class, component.getId());
        for (final Vulnerability vuln: component.getVulnerabilities()) {
            if (vuln.getSource() != null && vuln.getSource().equals(vulnerability.getSource())
                    && vuln.getVulnId() != null && vuln.getVulnId().equals(vulnerability.getVulnId())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns a List of all Vulnerabilities.
     * @return a List of Vulnerability objects
     */
    public PaginatedResult getVulnerabilities() {
        PaginatedResult result;
        final Query<Vulnerability> query = pm.newQuery(Vulnerability.class);
        if (orderBy == null) {
            query.setOrdering("id asc");
        }
        if (filter != null) {
            query.setFilter("vulnId.toLowerCase().matches(:vulnId)");
            final String filterString = ".*" + filter.toLowerCase() + ".*";
            result = execute(query, filterString);
        } else {
            result = execute(query);
        }
        for (final Vulnerability vulnerability: result.getList(Vulnerability.class)) {
            vulnerability.setAffectedProjectCount(this.getProjects(vulnerability).size());
            vulnerability.setAliases(getVulnerabilityAliases(vulnerability));
        }
        return result;
    }

    /**
     * Returns a List of Vulnerability for the specified Component and excludes suppressed vulnerabilities.
     * @param component the Component to retrieve vulnerabilities of
     * @return a List of Vulnerability objects
     */
    public PaginatedResult getVulnerabilities(Component component) {
        return getVulnerabilities(component, false);
    }

    /**
     * Returns a List of Vulnerability for the specified Component.
     * @param component the Component to retrieve vulnerabilities of
     * @return a List of Vulnerability objects
     */
    public PaginatedResult getVulnerabilities(Component component, boolean includeSuppressed) {
        PaginatedResult result;
        final String componentFilter = (includeSuppressed) ? "components.contains(:component)" : "components.contains(:component)" + generateExcludeSuppressed(component.getProject(), component);
        final Query<Vulnerability> query = pm.newQuery(Vulnerability.class);
        if (orderBy == null) {
            query.setOrdering("id asc");
        }
        if (filter != null) {
            query.setFilter(componentFilter + " && vulnId.toLowerCase().matches(:vulnId)");
            final String filterString = ".*" + filter.toLowerCase() + ".*";
            result = execute(query, component, filterString);
        } else {
            query.setFilter(componentFilter);
            result = execute(query, component);
        }
        for (final Vulnerability vulnerability: result.getList(Vulnerability.class)) {
            //vulnerability.setAffectedProjectCount(this.getProjects(vulnerability).size());
            vulnerability.setAliases(getVulnerabilityAliases(vulnerability));
        }
        return result;
    }

    /**
     * Returns a List of Vulnerability for the specified Component and excludes suppressed vulnerabilities.
     * This method if designed NOT to provide paginated results.
     * @param component the Component to retrieve vulnerabilities of
     * @return a List of Vulnerability objects
     */
    public List<Vulnerability> getAllVulnerabilities(Component component) {
        return getAllVulnerabilities(component, false);
    }

    /**
     * Returns a List of Vulnerability for the specified Component.
     * This method if designed NOT to provide paginated results.
     * @param component the Component to retrieve vulnerabilities of
     * @return a List of Vulnerability objects
     */
    @SuppressWarnings("unchecked")
    public List<Vulnerability> getAllVulnerabilities(Component component, boolean includeSuppressed) {
        final String filter = includeSuppressed ? "components.contains(:component)" : "components.contains(:component)" + generateExcludeSuppressed(component.getProject(), component);
        final Query<Vulnerability> query = pm.newQuery(Vulnerability.class, filter);
        final List<Vulnerability> vulnerabilities = (List<Vulnerability>)query.execute(component);
        for (final Vulnerability vulnerability: vulnerabilities) {
            //vulnerability.setAffectedProjectCount(this.getProjects(vulnerability).size());
            vulnerability.setAliases(getVulnerabilityAliases(vulnerability));
        }
        return vulnerabilities;
    }

    /**
     * Returns a List of Components affected by a specific vulnerability.
     * This method if designed NOT to provide paginated results.
     * @param project the Project to limit retrieval from
     * @param vulnerability the vulnerability to query on
     * @return a List of Component objects
     */
    public List<Component> getAllVulnerableComponents(Project project, Vulnerability vulnerability, boolean includeSuppressed) {
        final List<Component> components = new ArrayList<>();
        for (final Component component: getAllComponents(project)) {
            final Collection<Vulnerability> componentVulns = pm.detachCopyAll(
                    getAllVulnerabilities(component, includeSuppressed)
            );
            for (final Vulnerability componentVuln: componentVulns) {
                if (componentVuln.getUuid() == vulnerability.getUuid()) {
                    components.add(component);
                }
            }
        }
        return components;
    }

    /**
     * Returns the number of Vulnerability objects for the specified Project.
     * @param project the Project to retrieve vulnerabilities of
     * @return the total number of vulnerabilities for the project
     */
    public long getVulnerabilityCount(Project project, boolean includeSuppressed) {
        long total = 0;
        long suppressed = 0;
        final List<Component> components = getAllComponents(project);
        for (final Component component: components) {
            total += getCount(pm.newQuery(Vulnerability.class, "components.contains(:component)"), component);
            if (! includeSuppressed) {
                suppressed += getSuppressedCount(component); // account for globally suppressed components
                suppressed += getSuppressedCount(project, component); // account for per-project/component
            }
        }
        return total - suppressed;
    }

    /**
     * Returns a List of Vulnerability for the specified Project.
     * This method is unique and used by third-party integrations
     * such as ThreadFix for the retrieval of vulnerabilities from
     * a specific project along with the affected component(s).
     * @param project the Project to retrieve vulnerabilities of
     * @return a List of Vulnerability objects
     */
    public List<Vulnerability> getVulnerabilities(Project project, boolean includeSuppressed) {
        final List<Vulnerability> vulnerabilities = new ArrayList<>();
        final List<Component> components = getAllComponents(project);
        for (final Component component: components) {
            final Collection<Vulnerability> componentVulns = pm.detachCopyAll(
                    getAllVulnerabilities(component, includeSuppressed)
            );
            for (final Vulnerability componentVuln: componentVulns) {
                componentVuln.setComponents(Collections.singletonList(pm.detachCopy(component)));
                componentVuln.setAliases(new ArrayList<>(pm.detachCopyAll(getVulnerabilityAliases(componentVuln))));
            }
            vulnerabilities.addAll(componentVulns);
        }
        return vulnerabilities;
    }

    /**
     * Generates partial JDOQL statement excluding suppressed vulnerabilities for this project/component
     * and for globally suppressed vulnerabilities against the specified component.
     * @param component the component to query on
     * @param project the project to query on
     * @return a partial where clause
     */
    @SuppressWarnings("unchecked")
    private String generateExcludeSuppressed(Project project, Component component) {
        // Retrieve a list of all suppressed vulnerabilities
        final Query<Analysis> analysisQuery = pm.newQuery(Analysis.class, "project == :project && component == :component && suppressed == true");
        final List<Analysis> analysisList = (List<Analysis>)analysisQuery.execute(project, component);
        // Construct exclude clause based on above results
        String excludeClause = analysisList.stream().map(analysis -> "id != " + analysis.getVulnerability().getId() + " && ").collect(Collectors.joining());
        if (StringUtils.trimToNull(excludeClause) != null) {
            excludeClause = " && (" + excludeClause.substring(0, excludeClause.lastIndexOf(" && ")) + ")";
        }
        return excludeClause;
    }

    /**
     * Generates partial JDOQL statement excluding suppressed vulnerabilities for this project.
     * @param project the project to query on
     * @return a partial where clause
     */
    private String generateExcludeSuppressed(Project project) {
        return generateExcludeSuppressed(project, null);
    }

    /**
     * Returns a List of Projects affected by a specific vulnerability.
     * @param vulnerability the vulnerability to query on
     * @return a List of Projects
     */
    public List<Project> getProjects(Vulnerability vulnerability) {
        final List<Project> projects = new ArrayList<>();
        for (final Component component: vulnerability.getComponents()) {
            if (! super.hasAccess(super.principal, component.getProject())) {
                continue;
            }
            boolean affected = true;
            final Analysis projectAnalysis = getAnalysis(component, vulnerability);
            if (projectAnalysis != null && projectAnalysis.isSuppressed()) {
                affected = false;
            }
            if (affected) {
                projects.add(component.getProject());
            }
        }
        // Force removal of duplicates by taking the List and populating a Set and back again.
        final Set<Project> set = new LinkedHashSet<>(projects);
        projects.clear();
        projects.addAll(set);
        return projects;
    }

    public synchronized VulnerabilityAlias synchronizeVulnerabilityAlias(VulnerabilityAlias alias) {
        final Map<String, Object> params = new HashMap<>();
        String filter = "";
        if (alias.getCveId() != null) {
            filter += "(cveId == :cveId)";
            params.put("cveId", alias.getCveId());
        }
        if (alias.getSonatypeId() != null) {
            if (filter.length() > 0) filter += " && ";
            filter += "(sonatypeId == :sonatypeId || sonatypeId == null)";
            params.put("sonatypeId", alias.getSonatypeId());
        }
        if (alias.getGhsaId() != null) {
            if (filter.length() > 0) filter += " && ";
            filter += "(ghsaId == :ghsaId || ghsaId == null)";
            params.put("ghsaId", alias.getGhsaId());
        }
        if (alias.getOsvId() != null) {
            if (filter.length() > 0) filter += " && ";
            filter += "(osvId == :osvId || osvId == null)";
            params.put("osvId", alias.getOsvId());
        }
        if (alias.getSnykId() != null) {
            if (filter.length() > 0) filter += " && ";
            filter += "(snykId == :snykId || snykId == null)";
            params.put("snykId", alias.getSnykId());
        }
        if (alias.getGsdId() != null) {
            if (filter.length() > 0) filter += " && ";
            filter += "(gsdId == :gsdId || gsdId == null)";
            params.put("gsdId", alias.getGsdId());
        }
        if (alias.getVulnDbId() != null) {
            if (filter.length() > 0) filter += " && ";
            filter += "(vulnDbId == :vulnDbId || vulnDbId == null)";
            params.put("vulnDbId", alias.getVulnDbId());
        }
        if (alias.getInternalId() != null) {
            if (filter.length() > 0) filter += " && ";
            filter += "(internalId == :internalId || internalId == null)";
            params.put("internalId", alias.getInternalId());
        }
        final Query<VulnerabilityAlias> query = pm.newQuery(VulnerabilityAlias.class);
        query.setFilter(filter);
        final VulnerabilityAlias existingAlias = singleResult(query.executeWithMap(params));
        if (existingAlias != null) {
            existingAlias.copyFieldsFrom(alias);
            return persist(existingAlias);
        } else {
            return persist(alias);
        }
    }

    @SuppressWarnings("unchecked")
    public List<VulnerabilityAlias> getVulnerabilityAliases(Vulnerability vulnerability) {
        final Query<VulnerabilityAlias> query;
        if (Vulnerability.Source.NVD.name().equals(vulnerability.getSource())) {
            query = pm.newQuery(VulnerabilityAlias.class, "cveId == :cveId");
        } else if (Vulnerability.Source.OSSINDEX.name().equals(vulnerability.getSource())) {
            query = pm.newQuery(VulnerabilityAlias.class, "sonatypeId == :sonatypeId");
        } else if (Vulnerability.Source.GITHUB.name().equals(vulnerability.getSource())) {
            query = pm.newQuery(VulnerabilityAlias.class, "ghsaId == :ghsaId");
        } else if (Vulnerability.Source.OSV.name().equals(vulnerability.getSource())) {
            query = pm.newQuery(VulnerabilityAlias.class, "osvId == :osvId");
        } else if (Vulnerability.Source.SNYK.name().equals(vulnerability.getSource())) {
            query = pm.newQuery(VulnerabilityAlias.class, "snykId == :snykId");
        } else if (Vulnerability.Source.VULNDB.name().equals(vulnerability.getSource())) {
            query = pm.newQuery(VulnerabilityAlias.class, "vulnDb == :vulnDb");
        } else {
            query = pm.newQuery(VulnerabilityAlias.class, "internalId == :internalId");
        }
        return (List<VulnerabilityAlias>)query.execute(vulnerability.getVulnId());
    }

    /**
     * Reconcile {@link VulnerableSoftware} for a given {@link Vulnerability}.
     * <p>
     * {@link AffectedVersionAttribution}s are utilized to ensure that{@link VulnerableSoftware}
     * records are dropped that were previously reported by {@code source}, but aren't anymore.
     * <p>
     * {@link AffectedVersionAttribution}s are removed for a {@link VulnerableSoftware} record
     * if it is part of {@code vsListOld}, but not {@code vsList}.
     *
     * @param vulnerability The vulnerability this is about
     * @param vsListOld     Affected versions as previously reported
     * @param vsList        Affected versions as currently reported
     * @param source        The source who reported {@code vsList}
     * @return The reconciled {@link List} of {@link VulnerableSoftware}s
     * @since 4.7.0
     */
    public List<VulnerableSoftware> reconcileVulnerableSoftware(final Vulnerability vulnerability,
                                                                final List<VulnerableSoftware> vsListOld,
                                                                final List<VulnerableSoftware> vsList,
                                                                final Vulnerability.Source source) {
        if (vsListOld == null || vsListOld.isEmpty()) {
            return vsList;
        }

        for (final VulnerableSoftware vs : vsListOld) {
            final var vsPersistent = getObjectByUuid(VulnerableSoftware.class, vs.getUuid());
            if (vsPersistent == null) {
                continue; // Doesn't exist anymore
            } else if (vsList.contains(vsPersistent)) {
                continue; // We already have this one covered
            }

            final List<AffectedVersionAttribution> attributions = getAffectedVersionAttributions(vulnerability, vsPersistent);
            if (attributions.isEmpty()) {
                // DT versions prior to 4.7.0 did not record attributions.
                // Drop the VulnerableSoftware for now. If it was previously
                // reported by another source, it will be recorded and attributed
                // whenever that source is mirrored again.
                continue;
            }

            final boolean previouslyReportedBySource = attributions.stream()
                    .anyMatch(attr -> attr.getSource() == source);
            final boolean previouslyReportedByOthers = attributions.stream()
                    .anyMatch(attr -> attr.getSource() != source);

            if (previouslyReportedByOthers) {
                // Reported by another source, keep it.
                vsList.add(vsPersistent);
            }
            if (previouslyReportedBySource) {
                // Not reported anymore, remove attribution.
                deleteAffectedVersionAttribution(vulnerability, vsPersistent, source);
            }
        }

        return vsList;
    }

    /**
     * Fetch a {@link AffectedVersionAttribution} associated with a given
     * {@link Vulnerability}-{@link VulnerableSoftware} relationship.
     *
     * @param vulnerableSoftware the vulnerable software of the affected version attribution
     * @return a AffectedVersionAttribution object
     * @since 4.7.0
     */
    @Override
    public AffectedVersionAttribution getAffectedVersionAttribution(final Vulnerability vulnerability,
                                                                    final VulnerableSoftware vulnerableSoftware,
                                                                    final Vulnerability.Source source) {
        final Query<AffectedVersionAttribution> query = pm.newQuery(AffectedVersionAttribution.class, """
                vulnerability == :vulnerability && vulnerableSoftware == :vulnerableSoftware && source == :source
                """);
        query.setParameters(vulnerability, vulnerableSoftware, source);
        query.setRange(0, 1);
        return query.executeUnique();
    }

    /**
     * Fetch all {@link AffectedVersionAttribution}s associated with a given
     * {@link Vulnerability}-{@link VulnerableSoftware} relationship.
     *
     * @param vulnerability      The {@link Vulnerability} to fetch attributions for
     * @param vulnerableSoftware The {@link VulnerableSoftware} to fetch attributions for
     * @return A {@link List} of {@link AffectedVersionAttribution}s
     * @since 4.7.0
     */
    @Override
    public List<AffectedVersionAttribution> getAffectedVersionAttributions(final Vulnerability vulnerability,
                                                                           final VulnerableSoftware vulnerableSoftware) {
        final Query<AffectedVersionAttribution> query = pm.newQuery(AffectedVersionAttribution.class, """
                vulnerability == :vulnerability && vulnerableSoftware == :vulnerableSoftware
                """);
        query.setParameters(vulnerability, vulnerableSoftware);
        return query.executeList();
    }

    /**
     * Attributes multiple {@link Vulnerability}-{@link VulnerableSoftware} relationships to a given {@link Vulnerability.Source}.
     *
     * @param vulnerability The {@link Vulnerability} to update the attribution for
     * @param vsList        The {@link VulnerableSoftware}s to update the attribution for
     * @param source        The {@link Vulnerability.Source} to attribute
     * @see #updateAffectedVersionAttribution(Vulnerability, VulnerableSoftware, Vulnerability.Source)
     * @since 4.7.0
     */
    @Override
    public void updateAffectedVersionAttributions(final Vulnerability vulnerability,
                                                  final List<VulnerableSoftware> vsList,
                                                  final Vulnerability.Source source) {
        runInTransaction(() -> vsList.forEach(vs -> {
            AffectedVersionAttribution attribution = getAffectedVersionAttribution(vulnerability, vs, source);
            if (attribution == null) {
                attribution = new AffectedVersionAttribution(source, vulnerability, vs);
                pm.makePersistent(attribution);
            } else {
                attribution.setLastSeen(new Date());
            }
        }));
    }

    /**
     * Attributes a {@link Vulnerability}-{@link VulnerableSoftware} relationship to a given {@link Vulnerability.Source}.
     * <p>
     * If the attribution does not exist already, it is created.
     * If it does exist, the {@code lastSeen} timestamp is updated.
     *
     * @param vulnerability      The {@link Vulnerability} to update the attribution for
     * @param vulnerableSoftware The {@link VulnerableSoftware} to update the attribution for
     * @param source             The {@link Vulnerability.Source} to attribute
     * @since 4.7.0
     */
    @Override
    public void updateAffectedVersionAttribution(final Vulnerability vulnerability,
                                                 final VulnerableSoftware vulnerableSoftware,
                                                 final Vulnerability.Source source) {
        final AffectedVersionAttribution attribution = getAffectedVersionAttribution(vulnerability, vulnerableSoftware, source);
        if (attribution == null) {
            runInTransaction(() -> {
                final var newAttribution = new AffectedVersionAttribution(source, vulnerability, vulnerableSoftware);
                pm.makePersistent(newAttribution);
            });
        } else {
            runInTransaction(() -> attribution.setLastSeen(new Date()));
        }
    }

    /**
     * Delete a {@link AffectedVersionAttribution}.
     *
     * @param vulnerability      The {@link Vulnerability} to delete the attribution for
     * @param vulnerableSoftware The {@link VulnerableSoftware} to delete the attribution for
     * @param source             The {@link Vulnerability.Source} to delete the attribution for
     * @since 4.7.0
     */
    @Override
    public void deleteAffectedVersionAttribution(final Vulnerability vulnerability,
                                                 final VulnerableSoftware vulnerableSoftware,
                                                 final Vulnerability.Source source) {
        final Query<AffectedVersionAttribution> query = pm.newQuery(AffectedVersionAttribution.class);
        query.setFilter("""
                vulnerability == :vulnerability
                && vulnerableSoftware == :vulnerableSoftware
                && source == :source
                """);
        query.setParameters(vulnerability, vulnerableSoftware, source);
        query.deletePersistentAll();
    }

    /**
     * Delete all {@link AffectedVersionAttribution}s associated with a given {@link Vulnerability}.
     *
     * @param vulnerability The {@link Vulnerability} to delete {@link AffectedVersionAttribution}s for
     * @since 4.7.0
     */
    @Override
    public void deleteAffectedVersionAttributions(final Vulnerability vulnerability) {
        final Query<AffectedVersionAttribution> query = pm.newQuery(AffectedVersionAttribution.class);
        query.setFilter("vulnerability == :vulnerability");
        query.setParameters(vulnerability);
        query.deletePersistentAll();
    }

}