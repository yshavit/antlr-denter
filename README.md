> **Note**
> This project is archived, as I am no longer working on the project that requires it. It should still work — I just don't feel like updating its dependencies for upstream security patches. As of the time of this writing (April 2023), the only security vulnerabilities are in the unit tests.

Python-like indentation tokens for ANTLR4
=========================================

A mostly-readymade solution to INDENT/DEDENT tokens in ANTLR v4. Just plug in the `DenterHelper` and you'll be good to go! See [this blog post](http://blog.yuvalshavit.com/2014/02/python-like-indentation-using-antlr4.html) for some of the motivations behind this project.

antlr-helper is released under [the MIT license](http://opensource.org/licenses/MIT), which basically means you can do whatever you want with it. That said, I'd really appreciate hearing from you if you find this project useful! Maybe star the project?

Overview
========

This is a sort of "plugin" that you can splice into your antlr grammar's lexer, and which will then make that lexer work in a Python-like way with regards to expressions, statements, and blocks. It has three variants (for project written in Java, Python, and C#), described below.

Using the tokens in your parser
-------------------------------

When the denter injects `DEDENT` tokens, it'll prefix any string of them with a single `NL`. A single `NL` is also inserted before the EOF token if there are no `DEDENT`s to insert (that is, if the last line of the source file is not indented). A `NL` is _not_ inserted before an `INDENT`, since indents always imply a newline before them (and thus make the newline token meaningless). For example, given this input:

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

This approach lets you define expressions, single-line statements, and block statements naturally.

1. Expressions in your parser grammar should not end in newlines. This makes compound expressions work naturally.
2. Single-line statements in your grammar _should_ end in newlines. For example, an assignment expression might be `identifier '=' expression NL`.
3. Blocks are bookended by INDENT and DEDENT, without mentioning extra newlines: `block: INDENT statement+ DEDENT`.
   - You should _not_ include a newline before the INDENT
   - An `if` would be something like `if expression ':' block`. (Note the lack of `NL` after the `:`.)

In the example above, `universe` and `dolly` represent simple expressions, and you can imagine that the grammar would contain something like `statement: expression NL | helloBlock;`.

"Half-DEDENTs"
--------------

What happens when you dedent to an indentation level that was never established?

    someStatement()
    if foo():
          if bar():
              fooAndBar()
       bogusLine()

Notice that `bogusLine()` doesn't match with any indentation level: it's more indented than `if foo()` but less than its first statement, `if bar()`.

This is invalid in Python. If you to run such a program, you'll get:

> IndentationError: unindent does not match any outer indentation level

The `DenterHelper` processor handles this by inserting two tokens: a `DEDENT` followed immediately by an `INDENT` (the total sequence here would actually be two `DEDENT`s followed by an `INDENT`, since `bogusLine()` is twice-dedented from `fooAndBar()`). The rationale is that the line has dedened to its parent, and then indented. It's consistent with the indentation tokens for something like:

    someStatement()
      bogusLine()

If your indentation scheme is anything like python's, chances are you want this to be a compilation error. The good news is that it will be, as long as your parser doesn't allow "spontaneous" indents. That is, if the example just before this paragraph fails, then so will the half-dedent example above. In both cases, the parser rules will bork on an unexpected `INDENT` token.

How to use in  your host language
=================================

Java
----

![Java CI with Maven](https://github.com/yshavit/antlr-denter/workflows/Java%20CI%20with%20Maven/badge.svg)

    <dependency>
      <groupId>com.yuvalshavit</groupId>
      <artifactId>antlr-denter</artifactId>
      <version>1.1</version>
    </dependency>

1. Define INDENT and DEDENT tokens in your grammar
2. In your `@lexer::members` section, instantiate a `DenterHelper` whose `pullToken` method delegates to your lexer's `super.nextToken()`
3. Override your lexer's `super.nextToken` method to use `DenterHelper::nextToken` instead.
4. **Modify your `NL` token** to also grab any whitespace that follows (in other words, have it end in `' '*`, `'\t'*` or similar).

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

There is also a builder available, which is especially useful for Java 8:

    tokens { INDENT, DEDENT }
    
    @lexer::header {
      import com.yuvalshavit.antlr4.DenterHelper;
    }

    @lexer::members {
      private final DenterHelper denter = DenterHelper.builder()
        .nl(NL)
        .indent(MyCoolParser.INDENT)
        .dedent(MyCoolParser.DEDENT)
        .pullToken(MyCoolLexer.super::nextToken);
    
      @Override
      public Token nextToken() {
        return denter.nextToken();
      }
    }

    NL: ('\r'? '\n' ' '*);
	
Python3
-------

![Python package](https://github.com/yshavit/antlr-denter/workflows/Python%20package/badge.svg)

Big thanks to [@Bluepuff71](https://github.com/Bluepuff71) for porting this to Python!

```
pip install antlr-denter
```

Next, in Antlr put:
```
tokens { INDENT, DEDENT }

@lexer::header{
from antlr_denter.DenterHelper import DenterHelper
from MyCoolParser import MyCoolParser
}
@lexer::members {
class MyCoolDenter(DenterHelper):
    def __init__(self, lexer, nl_token, indent_token, dedent_token, ignore_eof):
        super().__init__(nl_token, indent_token, dedent_token, ignore_eof)
        self.lexer: MyCoolLexer = lexer

    def pull_token(self):
        return super(MyCoolLexer, self.lexer).nextToken()

denter = None

def nextToken(self):
    if not self.denter:
        self.denter = self.MyCoolDenter(self, self.NL, MyCoolParser.INDENT, MyCoolParser.DEDENT, ***Should Ignore EOF***)
    return self.denter.next_token()

}

NL: ('\r'? '\n' ' '*); #For tabs just switch out ' '* with '\t'*
```

CSharp
------

![Build/Tests](https://github.com/yshavit/antlr-denter/workflows/CSharp/badge.svg)

Credit to [@K2017](https://github.com/K2017) for this port.

In Antlr:
```antlrv4
tokens { INDENT, DEDENT }

@lexer::header {
using AntlrDenter.DenterHelper;
}

@lexer::members {
private DenterHelper denter;
  
public override IToken NextToken()
{
    if (denter == null)
    {
        denter = DenterHelper.Builder()
            .Nl(NL)
            .Indent(MyCoolParser.INDENT)
            .Dedent(MyCoolParser.DEDENT)
            .PullToken(base.NextToken);
    }

    return denter.NextToken();
}
}

NL: ('\r'? '\n' ' '*); #For tabs just switch out ' '* with '\t'*
```

Note the injected code is dedented with respect to the `@lexer::members` block. This is so that it has the proper formatting in the resulting C# Lexer file.

Repo layout
===========
- **Java/core**: The real thing. This is what you're interested in. Maven artifact `antlr-denter`.
- **Java/examples**: Contains a real-life example of a language that uses `DenterHelper`, so you can see a full solution, including the pom, how to set up the parser (which is nothing extra relative to usual antlr stuff) and how to define a language that uses these INDENT/DEDENT tokens. The language itself is pretty basic, but it should get the point across. Maven artifact `antlr-denter-example-examples`.
- **Python3**: The python3 implementation
- **CSharp**: The C# implementation; to be used as a class library in your projects.

The maven run is as simple as `mvn install` (or your favorite goal).

Comments? Suggestions? Bugs?
============================
Don't be shy about opening an issue!
