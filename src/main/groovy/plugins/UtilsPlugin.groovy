package plugins

import groovy.transform.CompileStatic
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.execution.TaskExecutionGraph
import tasks.TaskDependencyGraphTask

@CompileStatic
class UtilsPlugin implements Plugin<Project> {
    @Override
    void apply(Project project) {
        project.tasks.create("taskGraph", TaskDependencyGraphTask, "Show a task dependency graph.")
        configureTasks(project)
    }

    static void configureTasks(Project project) {
        project.gradle.taskGraph.whenReady { TaskExecutionGraph graph ->
            List<Task> tasks = graph.allTasks
            if (tasks.any { it instanceof TaskDependencyGraphTask }) {
                tasks.each { it.enabled = it instanceof TaskDependencyGraphTask }
            }
        }
    }
}
