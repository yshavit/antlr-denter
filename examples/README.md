Examples, and other use-case stuff
==================================

- `com.yuvalshavit.antlr4.examples.simplecalc`: A very simple expression calculator
- `com.yuvalshavit.antlr4.examples.grammarforker`: A tool for making two versions of a grammar, one based on indentation and the other based on C-style braces.
- `com.yuvalshavit.antlr4.examples.benchmark`: A very simple benchmark of `DenterHelper`.

**Note**: Any grammar file (`*.g4`) whose name starts with `antlrIgnore-` will _not_ be processed by the antlr plugin.

simplecalc
----------

Should be pretty self-explanatory. `SimpleCalcRunnerTest` tests the execution (using an antlr visitor). The various `.simplecalc` test files have an integer value on the first line, and the rest of the file is the actual `simplecalc` program. The test simply checks that the integer on that first line is equal to the program's result.

grammarforker
-------------

Includes a `main` for doing the fork. You give it a template file and an output directory, and it writes two versions of the grammar into that directory.

- The generated grammars will have a header which includes the `grammar MyGrammarName;` line, so don't include that in your template file.
- The grammars will be named _BaseGrammarName_Denting and _BaseGrammarName_Braced
- _BaseGrammarName_ is taken from the template file's name, ignoring any `antlrIgnore-` prefix and `.g4` suffix. For instance, a template file named `antlrIgnore-MyCoolLanguage.gf` will have a _BaseGrammarName_ of `MyCoolLanguage`, and will thus generate files `MyCoolLanguageDenting.g4` and `MyCoolLanguageBraced.g4`.

There is also a static method `GrammarForker::dentedToBraced` that translates an indentation-based program to a brace-based program. That is, you give it a lexer for:

    if foo()
      bar()
    else
      baz()

... and it'll return back a string:

    if foo() {
      bar()
    } else {
      baz()
    }

benchmark
---------

A really simple benchmark, based on a stupidly simple language. The language's source is `antlrIgnore-BenchGram.g4`. If you want to make a change to the language:

1. Modify `antlrIgnore-BenchGram.g4`.
2. Run `GrammarForker` to generate the dented/braced variants.
  - They should be in the same directory as `antlrIgnore-BenchGram.g4`. This is the default directory that `GrammarForker` will offer you, so you can just hit `<enter>` when it prompts you for an output directory.
3. Commit _all three_ files.
  - yes, it's ugly to have to check in generated resources, but I don't know of a simple way to have maven generate them before running antlr. I'm not a maven maven!

The benchmark itself will lex (but not parse or run) files named `XX-whateverName.benchdent`. The `XX` portion is parsed as a `double` and acts as a multiplier for how many times the program will be lexed as part of the benchmark. Smaller programs should have higher multipliers, so that each benchmark takes about equally long.

Each benchdent file is lexed three times:

- once using the indent lexer
- once using the braced lexer (with the `.benchdent` program translated using `GrammarForker::dentedToBraced`, described above)
- once using the "raw" tokens from the indent lexer -- that is, without `DenterHelper`'s overlay

Each of those has two phases, a 10-step warmup and a main run. They are identical except that the warmup has fewer iterations. The idea is that you can verify that the warmup has stabilized times (ie, that the JIT has done its work) before the main run. Note that each timed run also prints out a pseudorandom `int`. This is just a way of keeping the JIT honest and not letting it optimize out the lexing. Each token triggers a `nextInt()`, meaning that the JVM has to actually read the token, so that it can know how many times to invoke `nextInt()`.

On my machine, the results are pretty consistent:

- the dented version is _slightly_ faster than the braced version (!)
- the "raw" dented version is faster still, by about 20% (give or take)

In other words, the `DenterHelper` overhead adds about 20% to the run time, but this is more than offset by the fact that the raw dented programs are more than 20% faster than the braced programs!

This is a bit surprising at first, but it comes from the fact that the dented programs are shorter -- they don't need the opening ` {` (two chars, since we want nice formatting) or the closing `}` (including indentation). This may sound like a bit of cheating, but unless you're parsing minimized source code, it's actually a pretty real-world benefit.

Needless to say, all of that is subject to specific factors. Two that come to mind are lexing complexity and average line length within the program. Lexing complexity is important because the denting overhead should be pretty constant, and should therefore be less of a relative cost as the overall cost of lexing goes up. Average line length is important because it's basically a ratio of denting tokens to non-denting tokens; as lines get longer, I would expect the relative overhead of denting to go down.

As a simple test of that last hypothesis, one of the benchmarks is of a program with all short lines: `a = b();` etc. Here are the dented:braced ratios for all of the benchmarks on my machine (you can see [all the data on google drive](http://goo.gl/f9yAQw)). Lower numbers are better; anything below 100% means the dented version is faster than the braced version.

    small       99%
    medium      99%
    large       98%
    shortLines  113%

As expected, short lines (and thus a high amount of denting relative to non-dent tokens) increases the cost of denting. With these trivially short lines (and no indentation, and thus no braces to make the braced version of the code be longer), the overhead is significant enough that the dented version is slower than the braced version. Obviously, this is a pretty extreme case.

The above all suggests that, when all is said and done, the real-world impact on lexing is probably negligible.

