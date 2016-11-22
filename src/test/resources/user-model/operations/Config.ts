import {PathExpressionEngine} from './PathExpression'

/**
  Common context for all classes that need it
*/
interface Config {

  eng(): PathExpressionEngine

}

export { Config }
