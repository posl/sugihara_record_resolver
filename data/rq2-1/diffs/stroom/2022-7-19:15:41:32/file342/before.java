/*
 * This file is generated by jOOQ.
 */
package stroom.job.impl.db.jooq;


import stroom.job.impl.db.jooq.tables.Job;
import stroom.job.impl.db.jooq.tables.JobNode;

import javax.annotation.processing.Generated;


/**
 * Convenience access to all tables in stroom
 */
@Generated(
    value = {
        "http://www.jooq.org",
        "jOOQ version:3.12.3"
    },
    comments = "This class is generated by jOOQ"
)
@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class Tables {

    /**
     * The table <code>stroom.job</code>.
     */
    public static final Job JOB = Job.JOB;

    /**
     * The table <code>stroom.job_node</code>.
     */
    public static final JobNode JOB_NODE = JobNode.JOB_NODE;
}
