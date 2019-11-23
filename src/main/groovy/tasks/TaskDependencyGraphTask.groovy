package tasks

import groovy.transform.CompileStatic
import guru.nidi.graphviz.attribute.Color
import guru.nidi.graphviz.attribute.Style
import guru.nidi.graphviz.engine.Format
import guru.nidi.graphviz.engine.Graphviz
import guru.nidi.graphviz.model.MutableGraph
import guru.nidi.graphviz.model.MutableNode
import org.gradle.api.DefaultTask
import org.gradle.api.Task
import org.gradle.api.internal.tasks.DefaultTaskDependency
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction

import static guru.nidi.graphviz.model.Factory.*

/**
 * This task will create a execution-dependency-time graph in which each node has links to its finalizer and dependent tasks.
 * Dependency links will represent tasks that will execute BEFORE the task executes.
 * Finalizer links will represent tasks that will execute AFTER the task executes.
 */
@CompileStatic
class TaskDependencyGraphTask extends DefaultTask {
    @Internal
    @Lazy
    private MutableGraph dependencyGraph = mutGraph("Task Dependency Graph").setDirected(true)

    @Internal
    @Lazy
    private File outputFile = project.file("build/image.png")

    @TaskAction
    void run() {
        project
                .gradle
                .startParameter
                .taskNames
                .collect {
                    project.tasks.getByName(it)
                }
                .each {
                    Task requestedTask = (Task) it
                    if (requestedTask.name != this.name) {
                        MutableNode root = mutNode(requestedTask.name)
                                .add(Color.DARKOLIVEGREEN)
                                .add(Style.BOLD)
                        recurse(requestedTask, root)
                    }
                }
        renderGraphToFile()
    }

    /**
     * Recursively add nodes to the task graph.
     * @param task
     * @param root
     */
    void recurse(Task task, MutableNode root) {
        getTaskDependencies(task).each {
            MutableNode node = mutNode(it.name)
            recurse(it, node)
            addLink(node, root)
        }
        dependencyGraph.add(root)
        getTaskFinalizers(task).each {
            MutableNode node = mutNode(it.name)
            addLink(root, node)
            recurse(it, node)
        }
    }

    /**
     * Adds a link to the given node and adds the node (and link) to the graph.
     * @param node
     * @param link
     * @param colour
     */
    void addLink(MutableNode node, MutableNode link) {
        dependencyGraph.add(node.addLink(link))
    }

    /**
     * This will get tasks that are scheduled to execute *BEFORE* {@param task} is executed.
     * @param task
     */
    static Set<Task> getTaskDependencies(Task task) {
        return task.dependsOn as Set<Task>
    }

    /**
     * This will get tasks that are scheduled to execute *AFTER* {@param task} is executed.
     * @param task
     */
    static Set<Task> getTaskFinalizers(Task task) {
        return (task.finalizedBy as DefaultTaskDependency).mutableValues as Set<Task>
    }

    /**
     * Renders the graph and stores it in a file.
     */
    void renderGraphToFile() {
        println("Rendering graph and sending output to file...")
        Graphviz
                .fromGraph(dependencyGraph)
                .render(Format.PNG)
                .toFile(outputFile)
        println("Graph was rendered to file: ${outputFile.absolutePath}.")
    }
}
