import {Project} from '@atomist/rug/model/Core'
import {ProjectEditor} from '@atomist/rug/operations/ProjectEditor'
import {PathExpression} from '@atomist/rug/tree/PathExpression'
import {PathExpressionEngine} from '@atomist/rug/tree/PathExpression'
import {Match} from '@atomist/rug/tree/PathExpression'
import {File} from '@atomist/rug/model/Core'
import {Result,Status, Parameter} from '@atomist/rug/operations/RugOperation'

class ConstructedEditor implements ProjectEditor {
    name: string = "Constructed"
    description: string = "A nice little editor"
    tags: string[] = ["java", "maven"]

    parameters: Parameter[] = [{name: "packageName", description: "The Java package name", displayName: "Java Package", pattern: "^.*$", maxLength: 100}]
    edit(project: Project, {packageName } : { packageName: string}) {

      let eng: PathExpressionEngine = project.context().pathExpressionEngine();

      var t: string = `param=${packageName},filecount=${project.fileCount()}`

      eng.with<File>(project, "->file", n => {
        t += `Matched file=${n.path()}`;
        n.append("randomness")
      })

        var s: string = ""

        project.addFile("src/from/typescript", "Anders Hjelsberg is God");
        for (let f of project.files())
            s = s + `File [${f.path()}] containing [${f.content()}]\n`
        return new Result(Status.Success,
        `${t}\n\nEdited Project containing ${project.fileCount()} files: \n${s}`)
    }
  }
  var editor = new ConstructedEditor()