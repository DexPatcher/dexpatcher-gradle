/*
 * DexPatcher - Copyright 2015-2019 Rodrigo Balerdi
 * (GNU General Public License version 3 or later)
 *
 * DexPatcher is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published
 * by the Free Software Foundation, either version 3 of the License,
 * or (at your option) any later version.
 */

package lanchon.dexpatcher.gradle

import groovy.transform.CompileStatic

import org.gradle.api.Action
import org.gradle.api.Task
import org.gradle.api.tasks.TaskProvider

@CompileStatic
abstract class TaskGraphHelper {

    static void beforeTask(Task task, Action<? super Task> action) {
        task.project.gradle.taskGraph.beforeTask {
            if (it.is(task)) action.execute task
        }
    }

    static void beforeTask(TaskProvider task, Action<? super Task> action) {
        task.configure { Task it ->
            beforeTask it, action
        }
    }

    static void afterTask(Task task, Action<? super Task> action) {
        task.project.gradle.taskGraph.afterTask {
            if (it.is(task)) action.execute task
        }
    }

    static void afterTask(TaskProvider task, Action<? super Task> action) {
        task.configure { Task it ->
            afterTask it, action
        }
    }

}
