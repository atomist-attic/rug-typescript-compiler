interface Project {

    fileCount(): number

    addFile(path: string, content: string)

    files(): Array<File>

    copyFileOrFail(name: string, sourcePath: string, definitionPath: string)
}

interface File {

    name(): string

    path(): string

    content(): string

    append(what: string): void
    
    prepend(what: string): void
}

export { Project }
export { File }
