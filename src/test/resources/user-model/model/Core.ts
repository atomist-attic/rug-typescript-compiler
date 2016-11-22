interface Project {

    fileCount(): number

    addFile(path: string, content: string)

    files(): Array<File>

    copyFileOrFail(name: string, sourcePath: string, definitionPath: string)
}

interface File {

    path(): string

    content(): string
}

export { Project }
export { File }
