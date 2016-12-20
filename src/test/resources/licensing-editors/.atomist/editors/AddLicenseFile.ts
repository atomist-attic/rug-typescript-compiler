/*
 * Copyright © 2016 Atomist, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import { Project, File, Yml } from '@atomist/rug/model/Core'
import { ProjectEditor } from '@atomist/rug/operations/ProjectEditor'
import { Result, Status, Parameter} from '@atomist/rug/operations/RugOperation'

let params: Parameter[] = [
        {required: true,
          description: "the name of the license to add to project",
          displayName: "License Name",
          validInput: "the name of a license template without the .yml extension, see https://github.com/atomist-rugs/licensing-editors/tree/master/.atomist/templates",
          pattern: "^\\w[-\\w.]*$",
          minLength: 1,
          maxLength: 20,
          name: "license_name"}
]

//return true if file is a license file
function isLicense(f: File) {
    let path = f.path().toLowerCase()
    return path == "license" || path == "license.txt" || path == "license.md";
}

let editor: ProjectEditor = {
    tags: ["license", "licensing", "copyright", "documentation"],
    name: "AddLicenseFile",
    description: "Add a license fiel to a project",
    parameters: params,
    edit(project: Project, {license_name} : {license_name: string}) {

        let licenseFileName = ".atomist/templates/" + license_name + ".yml"
        let licenseFile = project.backingArchiveProject().findFile(licenseFileName)
        if (licenseFile == null) {
            throw Error(`Unable to find licenseFile: ${licenseFileName}`)
        }
        let strings = licenseFile.content().split("---")

        let licenseFiles: File[] = project.files().filter(isLicense)

        if (licenseFiles.length < 1) {
            console.log("  Adding LICENSE file...")
            project.addFile("LICENSE", strings[2])
        } else if (licenseFiles.length == 1) {
            console.log("  Updating LICENSE file...")
            licenseFiles[0].setContent(strings[2])
        } else {
            throw Error(`Found too many license files wasn't sure what to do`)
        }
        return new Result(Status.Success, "License file added")
    }
}
