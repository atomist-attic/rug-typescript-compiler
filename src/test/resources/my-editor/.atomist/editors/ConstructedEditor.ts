import {Project} from 'user-model/model/Core'
import {ParametersSupport} from 'user-model/operations/ProjectEditor'
import {ProjectEditor} from 'user-model/operations/ProjectEditor'
import {Parameters} from 'user-model/operations/ProjectEditor'
import {PathExpression} from 'user-model/operations/PathExpression'
import {PathExpressionEngine} from 'user-model/operations/PathExpression'
import {Match} from 'user-model/operations/PathExpression'
import {File} from 'user-model/model/Core'

import {parameter} from 'user-model/support/Metadata'
import {inject} from 'user-model/support/Metadata'
import {parameters} from 'user-model/support/Metadata'
import {tag} from 'user-model/support/Metadata'
import {editor} from 'user-model/support/Metadata'

abstract class JavaInfo extends ParametersSupport {

  @parameter({description: "The Java package name", displayName: "Java Package", pattern: ".*", maxLength: 100})
  packageName: string = null

}
@editor("A nice little editor")
@tag("java")
@tag("maven")
class ConstructedEditor implements ProjectEditor<Parameters> {

    @inject("PathExpressionEngine")
    eng: PathExpressionEngine

    edit(project: Project, @parameters("JavaInfo") ji: JavaInfo) {

      let pe = new PathExpression<Project,File>(`/*:file[name='pom.xml']`)
      let m: Match<Project,File> = this.eng.evaluate(project, pe)

      var t: string = `param=${ji.packageName},filecount=${m.root().fileCount()}`
      for (let n of m.matches())
        t += `Matched file=${n.path()}`;

        var s: string = ""

        project.addFile("src/from/typescript", "Anders Hjelsberg is God");
        for (let f of project.files())
            s = s + `File [${f.path()}] containing [${f.content()}]\n`
        return `${t}\n\nEdited Project containing ${project.fileCount()} files: \n${s}`;
    }
  }
