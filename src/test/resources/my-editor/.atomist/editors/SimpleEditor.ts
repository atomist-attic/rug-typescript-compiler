import {Project} from '@atomist/rug/model/Core'
import {ProjectEditor} from '@atomist/rug/operations/ProjectEditor'
import {Parameter, Result, Status} from '@atomist/rug/operations/RugOperation'
import {File} from '@atomist/rug/model/Core'

class SimpleEditor implements ProjectEditor {

    name: string = "Simple"
    description: string = "A nice little editor"
    tags: string[] = ["java", "maven"]
    parameters: Parameter[] = [
        {name: "content", description: "Content", displayName: "content", pattern: "$ContentPattern", maxLength: 100, default: "Anders"},
        {name: "num", description: "some num", displayName: "num", pattern: "^[\\d]+$$", maxLength: 100, default: 10}
    ]

    edit(project: Project, {content, num }: {content: string, num: number}): Result {
      project.addFile("src/from/typescript", content);
      return new Result(Status.Success,
      `Edited Project now containing $${project.fileCount()} files: \n`);
    }
  }
var myeditor = new SimpleEditor()