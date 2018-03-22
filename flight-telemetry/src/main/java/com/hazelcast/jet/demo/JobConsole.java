package com.hazelcast.jet.demo;

import com.hazelcast.jet.JetInstance;
import com.hazelcast.jet.Job;
import com.hazelcast.jet.config.JobConfig;
import com.hazelcast.jet.pipeline.Pipeline;
import org.apache.log4j.Logger;
import org.python.apache.commons.compress.utils.Charsets;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * Serves a command line console to submit and track the given pipeline. You
 * can submit the given pipeline or get a reference to a job that is submitted
 * by another node. Once you have a job reference, you can query the job
 * status. You can also cancel or restart the job. You can still query the job
 * status after the job is completed, since the job reference is not deleted
 * automatically. To delete the job reference, type "reset".
 * Type "help" to see the available commands.
 */
public class JobConsole implements Runnable {

    private static final Logger LOGGER = Logger.getLogger(JobConsole.class);


    private final BufferedReader reader = new BufferedReader(new InputStreamReader(System.in, Charsets.UTF_8));

    private final JetInstance jet;

    private final JobConfig jobConfig;

    private final Pipeline pipeline;

    private Job job;

    JobConsole(JetInstance jet, Pipeline pipeline, JobConfig jobConfig) {
        this.jet = jet;
        this.pipeline = pipeline;
        this.jobConfig = jobConfig;
        if (this.jobConfig.getName() == null) {
            throw new IllegalArgumentException("The job has no name!");
        }
    }

    @Override
    public void run() {
        LOGGER.info("Job console has started. Type \"help\" to see available commands...");
        String command;
        while ((command = readCommand()) != null) {
            try {
                process(command);
            } catch (Exception e) {
                LOGGER.error(command + " failed", e);
            }
        }
    }

    private void process(String command) {
        switch (command) {
            case "submit":
                submitJob();
                break;
            case "get":
                getJob();
                break;
            case "status":
                queryJobStatus();
                break;
            case "cancel":
                cancelJob();
                break;
            case "restart":
                restartJob();
                break;
            case "reset":
                deleteJob();
                break;
            case "help":
                LOGGER.info("Available commands:");
                LOGGER.info("submit: submits the job to the cluster");
                LOGGER.info("get: gets a reference to the job which is submitted by some other node");
                LOGGER.info("status: queries status of the job");
                LOGGER.info("cancel: cancels the job");
                LOGGER.info("restart: restarts the job");
                LOGGER.info("reset: deletes the current job reference");
                break;
            case "":
                break;
            default:
                LOGGER.error("INVALID COMMAND: " + command
                        + ". VALID COMMANDS: help|submit|get|status|cancel|restart|reset");
        }
    }

    private String readCommand() {
        System.out.println("$");
        try {
            return reader.readLine().trim().toLowerCase();
        } catch (IOException e) {
            LOGGER.error("Error while reading command", e);
            return null;
        }
    }

    private void submitJob() {
        if (job == null) {
            job = jet.newJob(pipeline, jobConfig);
            LOGGER.info(jobIdString() + " has been submitted.");
        } else {
            LOGGER.error("There is already a reference for " + jobIdString() + ".");
        }
    }

    private void getJob() {
        if (job == null) {
            job = jet.getJob(jobConfig.getName());
            if (job != null) {
                LOGGER.info(jobIdString() + " reference has been fetched.");
            } else {
                LOGGER.error("Job not found.");
            }
        } else {
            LOGGER.error("There is already a reference for " + jobIdString() + ".");
        }
    }

    private void queryJobStatus() {
        if (job != null) {
            LOGGER.info(jobIdString() + " status: " + job.getStatus());
        } else {
            LOGGER.error("There is no job reference.");
        }
    }

    private void cancelJob() {
        if (job != null) {
            if (job.cancel()) {
                LOGGER.info(jobIdString() + " has been cancelled.");
            } else {
                LOGGER.error(jobIdString() + " not cancelled. It is " + job.getStatus());
            }
        } else {
            LOGGER.error("There is no job reference.");
        }
    }

    private void restartJob() {
        if (job != null) {
            if (job.restart()) {
                LOGGER.info(jobIdString() + " is restarting.");
            } else {
                LOGGER.error(jobIdString() + " is not restarted.");
            }
        } else {
            LOGGER.error("There is no job reference.");
        }
    }

    private void deleteJob() {
        if (job != null) {
            LOGGER.info(jobIdString() + " reference is reset.");
            job = null;
        } else {
            LOGGER.error("There is no job reference.");
        }
    }

    private String jobIdString() {
        return "Job[" + job.getId() + "]";
    }

}
