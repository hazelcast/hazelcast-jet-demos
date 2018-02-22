package com.hazelcast.jet.demo;

import com.hazelcast.jet.Jet;
import com.hazelcast.jet.JetInstance;
import com.hazelcast.jet.Job;
import com.hazelcast.jet.core.JobStatus;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

/**
 * It starts a new Jet instance and restart the currently running job to scale it to run on all members
 * including the new launched one.
 */
public class AdditionalMember {
    public static void main(String[] args) {
        System.setProperty("hazelcast.logging.type", "log4j");
        JetInstance jet = Jet.newJetInstance();
        Job job = jet.getJob(FlightTelemetry.JOB_NAME);

        makeSureThatJobExists(job);
        makeSureThatJobIsRunning(job);

        job.restart();
        job.join();
    }

    private static void makeSureThatJobExists(Job job) {
        if (job == null) {
            throw new IllegalStateException("Job(" + FlightTelemetry.JOB_NAME + ") cannot be found. Are you sure that you\'ve started the FlightTelemetry ?");
        }
    }

    private static void makeSureThatJobIsRunning(Job job) {
        while (!JobStatus.RUNNING.equals(job.getStatus())) {
            LockSupport.parkNanos(TimeUnit.SECONDS.toNanos(1));
            System.out.println("Waiting for Job(" + FlightTelemetry.JOB_NAME + ")  to be started.");
        }
    }
}
