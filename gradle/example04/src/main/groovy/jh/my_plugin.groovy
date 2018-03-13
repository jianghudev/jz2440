package jh
import org.gradle.api.Plugin
import org.gradle.api.Project


class my_plugin implements Plugin<Project>{
    void apply(Project p){
        p.task('my_2_p_task') << {
            println "自定义的独立插件，可以给别人用，在插件中定义的1个task "
        }
    }
}
