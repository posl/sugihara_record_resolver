package org.asamk.signal.manager.internal;

import org.asamk.signal.manager.helper.Context;
import org.asamk.signal.manager.jobs.Job;

public class JobExecutor {

    private final Context context;

    public JobExecutor(final Context context) {
        this.context = context;
    }

    public void enqueueJob(Job job) {
        job.run(context);
    }
}
