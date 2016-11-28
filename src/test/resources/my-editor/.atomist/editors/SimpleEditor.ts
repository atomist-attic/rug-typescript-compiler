import {ProjectEditor} from '@atomist/rug/operations/ProjectEditor'
import {Parameters} from '@atomist/rug/operations/Parameters'
import {Project} from '@atomist/rug/model/Core'
import {Status, Result} from '@atomist/rug/operations/Result'

/**
  Simple editor with no parameters
*/
class SimpleEditor implements ProjectEditor<Parameters> {

    edit(project: Project, p: Parameters) {
        project.addFile("src/from/typescript", "Anders Hjelsberg is God");
        return new Result(Status.Success, `Edited Project now containing ${project.fileCount()} files: \n`);
    }
}