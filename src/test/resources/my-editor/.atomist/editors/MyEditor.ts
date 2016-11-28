interface Project {


    fileCount(): number

    addFile(path: string, content: string)

    files(): Array<File>
}

interface File {

  path(): string

  content(): string
}


class Match<R,N> {

  _root: R;
  _matches: Array<N>;

  constructor(root: R, matches: Array<N>) {
    this._root = root
    this._matches = matches;
  }

// Nashorn needs methods
  root(): R { return this._root; }
  matches(): Array<N> { return this._matches;}
}

class PathExpression<R,N> {

  constructor(public expression: string) {}

}

interface PathExpressionEngine {

  evaluate<R,N>(root, expr: PathExpression<R,N>): Match<R,N>
}

// TODO Nashorn doesn't seem to like exports

interface ProjectEditor {

    edit(project: Project, eng: PathExpressionEngine): string
}

function set_metadata(obj: any, key: string, value: any){
  Object.defineProperty(obj, key, {value: value, writable: false, enumerable: false})
}

function get_metadata(obj: any, key: string){
   let desc =  Object.getOwnPropertyDescriptor(obj, key);
   if((desc == null || desc == undefined) && (obj.prototype != undefined)){
     desc = Object.getOwnPropertyDescriptor(obj.prototype, key);
   }
   if(desc != null || desc != undefined){
     return desc.value;
   }
   return null;
}

/**
Remember that all interface, generic and type information is totally lost, so it can't be enforced anyway.

So extension/implementation of our interfaces is entirely optional, though recommended for ease of use/compile time checking.
**/

function editor(description :string){
  return function (cons: Function){
    set_metadata(cons,"rug-type","editor");
    set_metadata(cons,"editor-description",description);
  }
}

function tag(name :string){
  return function (cons: Function){
    let tags: [string] = get_metadata(cons, "tags");
    if(tags == null){
      tags = [name];
    }else if(tags.indexOf(name) < 0){
      tags.push(name)
    }
    set_metadata(cons,"tags",tags);
  }
}

function project(){
  return function (target: any, name: string) : void {
    set_metadata(target,"project-key",name);
  };
}
function inject(name: string){
  return function (target: any, propertyKey: string) {
    let bindings: {} = get_metadata(target, "bindings");

    if(bindings == null){
      bindings = {}
    }
    bindings[propertyKey] = name;
    set_metadata(target, "bindings", bindings);
  }
}

function param(details: any){
  return function (target: any, propertyKey: string) {
    let params: {} = get_metadata(target, "params");

    if(params == null){
      params = {}
    }
    params[propertyKey] = details;
    set_metadata(target, "params", params);
  }
}

function expose (target: any, propertyKey: string, descriptor: PropertyDescriptor) {
  set_metadata(target,"expose",propertyKey);
}

//below would be provided by Editor author

@editor("Does some editing")
@tag("clojure")
@tag("fun")
class MyEditor {

    @inject("Project")
    private project: Project

    @inject("PathExpressionEngine")
    private eng: PathExpressionEngine

    @param({description: "A Java package name", displayName: "Package Name", maxLength: 10, pattern: "*."})
    packageName: string

    @expose
    edit() {

      let pe = new PathExpression<Project,File>(`/*:file[name='pom.xml']`)
      let m: Match<Project,File> = this.eng.evaluate(this.project, pe)

      var t: string = `param=${this.packageName},filecount=${m.root().fileCount()}`
      for (let n of m.matches())
        t += `Matched file=${n.path()}`;

        var s: string = ""

        this.project.addFile("src/from/typescript", "Anders Hjelsberg is God");
        for (let f of this.project.files())
            s = s + `File [${f.path()}] containing [${f.content()}]\n`
        return `${t}\n\nEdited Project containing ${this.project.fileCount()} files: \n${s}`;
    }
}
