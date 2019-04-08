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
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.Transformer
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.TaskProvider

@CompileStatic
class ExistingTaskProvider<T extends Task> implements TaskProvider<T> {

    private final T task
    private final Provider<T> provider

    ExistingTaskProvider(Project project, T task) {
        this.task = task;
        provider = project.<T>provider { task }
    }

    @Override String getName() { task.name }
    @Override void configure(Action<? super T> action) { action.execute task }
    @Override T get() { task }
    @Override T getOrNull() { task }
    @Override T getOrElse(T defaultValue) { task }
    @Override boolean isPresent() { true }

    @Override <S> Provider<S> map(Transformer<? extends S, ? super T> transformer) { provider.map transformer }

}
