Python-like indentation tokens for ANTLR4
=========================================

A mostly-readymade solution to INDENT/DEDENT tokens in ANTLR v4. Just plug in the `DenterHelper` and you'll be good to go!

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

Basically, just use them. One bit worth noting is that when the denter injects DEDENT tokens, it'll prefix any string of them with a single `NL`. For instance, given this input:

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
    &lt;eof&gt;

This is done so that simple expressions can be terminated by the `NL` token. In this case, `universe` represents a simple expression, and you can imagine that the parser would define it as something like `universeExpr: 'universe' NL`. Easy peasy!

Repo layout, maven stuff
========================

tl;dr, for maven:

    <dependency>
      <groupId>com.yuvalshavit.antlr4</groupId>
      <artifactId>denter</artifactId>
      <version>1.0-SNAPSHOT</version>
    </dependency>

- **core**: The real thing. This is what you're interested in.
- **examples**: Contains a real-life example of a language that uses `DenterHelper`, so you can see a full solution, including the pom, how to set up the parser (which is nothing extra relative to usual antlr stuff) and how to define a language that uses these INDENT/DEDENT tokens. The language itself is pretty basic, but it should get the point across.

Comments? Suggestions? Bugs?
============================
Don't be shy about opening an issue!

