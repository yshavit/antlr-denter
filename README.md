Python-like indentation tokens for ANTLR4
=========================================

A mostly-readymade solution to INDENT/DEDENT tokens in ANTLR v4. Just plug in the `DenterHelper` and you'll be good to go! See [this blog post](http://blog.yuvalshavit.com/2014/02/python-like-indentation-using-antlr4.html) for some of the motivations behind this project.

antlr-helper is released under [the MIT license](http://opensource.org/licenses/MIT), which basically means you can do whatever you want with it. That said, I'd really appreciate hearing from you if you find this project useful! Maybe star the project?

Usage
=====

Adding INDENT / DEDENT tokens to your lexer
-------------------------------------------

1. Define INDENT and DEDENT tokens in your grammar
2. In your `@lexer::members` section, instantiate a `DenterHelper` whose `pullToken` method delegates to your lexer's `super.nextToken()`
3. Override your lexer's `super.nextToken` method to use `DenterHelper::nextToken` instead.
4. Modify your `NL` token to also grab any whitespace that follows

`DenterHelper` is an abstract class, and it also takes three arguments for its constructor: the token types for newline, INDENT and DEDENT. It's probably easiest to instantiate it as an anonymous class. The whole thing should look something like this:

    tokens { INDENT, DEDENT }
    
    @lexer::header {
      import com.yuvalshavit.antlr4.DenterHelper;
    }

    @lexer::members {
      private final DenterHelper denter = new DenterHelper(NL,
                                                           MyCoolParser.INDENT,
                                                           MyCoolParser.DEDENT)
      {
        @Override
        public Token pullToken() {
          return MyCoolLexer.super.nextToken();
        }
      };
    
      @Override
      public Token nextToken() {
        return denter.nextToken();
      }
    }

    NL: ('\r'? '\n' ' '*);

Using the tokens in your parser
-------------------------------

Basically, just use them. One bit worth noting is that when the denter injects DEDENT tokens, it'll prefix any string of them with a single `NL`. A single `NL` is also inserted before the EOF token if there are no DEDENTs to insert. For instance, given this input:

    hello
      world
        universe
    dolly

... the tokens would be (roughly):

    "hello"
    INDENT
    "world"
    INDENT
    "universe"
    NL
    DEDENT
    DEDENT
    "dolly"
    NL
    <eof>

This is done so that simple expressions can be terminated by the `NL` token without worrying about surrounding context (an impending dedent or EOF). In this case, `universe` and `dolly` represent simple expressions, and you can imagine that the grammar would contain something like `statement: expr  NL | helloBlock;`. Easy peasy!

"Half-DEDENTs"
--------------

What happens when you dedent to an indentation level that was never established?

    someStatement()
    if foo():
        if bar():
          fooAndBar()
      bogusLine()

Notice that `bogusLine()` doesn't match with any indentation level: it's more indented than `if foo()` but less than its first statement, `if bar()`.

This is a buggy program in python. If you to run such a program, you'll get:

> IndentationError: unindent does not match any outer indentation level

The `DenterHelper` processor handles this by inserting two tokens: a `DEDENT` followed immediately by an `INDENT` (the total sequence here would actually be two `DEDENT`s followed by an `INDENT`, since `bogusLine()` is twice-dedented from `fooAndBar()`). The rationale is that the line has dedened to its parent, and then indented. It's consistent with the indentation tokesns for something like:

    someStatement()
      bogusLine()

If your indentation scheme is anything like python's, chances are you want this to be a compilation error. The good news is that it will be, as long as your parser doesn't allow "spontaneous" indents. That is, if the example just before this paragraph fails, then so will the half-dedent example above. In both cases, the parser rules will bork on an unexpected `INDENT` token.

Repo layout, maven stuff
========================

tl;dr, for maven:

    <dependency>
      <groupId>com.yuvalshavit.antlr4</groupId>
      <artifactId>antlr-denter</artifactId>
      <version>1.0-SNAPSHOT</version>
    </dependency>

- **core**: The real thing. This is what you're interested in.
- **examples**: Contains a real-life example of a language that uses `DenterHelper`, so you can see a full solution, including the pom, how to set up the parser (which is nothing extra relative to usual antlr stuff) and how to define a language that uses these INDENT/DEDENT tokens. The language itself is pretty basic, but it should get the point across.

The maven run is as simple as `mvn install` (or your favorite goal).

Comments? Suggestions? Bugs?
============================
Don't be shy about opening an issue!

