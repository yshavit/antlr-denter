package com.yuvalshavit.antlr4.examples.simplecalc;

public class SimpleCalcRunner extends SimpleCalcBaseVisitor<Integer> {
  @Override
  public Integer visitOperation(SimpleCalcParser.OperationContext ctx) {
    Integer operand0 = visit(ctx.expr(0));
    Integer operand1 = visit(ctx.expr(1));
    switch (ctx.OP().getText().toLowerCase()) {
    case "add": return operand0 + operand1;
    case "multiply": return operand0 * operand1;
    case "sub": return operand0 - operand1;
    default: throw new UnknownOperandException(ctx.OP().getText());
    }
  }

  @Override
  public Integer visitIntLiteral(SimpleCalcParser.IntLiteralContext ctx) {
    return Integer.parseInt(ctx.INT().getText());
  }

  public static class UnknownOperandException extends RuntimeException {
    public UnknownOperandException(String name) {
      super("unknown calculation: " + name);
    }
  }
}
