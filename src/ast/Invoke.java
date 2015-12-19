package ast;

public class Invoke extends Expression {

	public Invoke(Expression func, ExpressionGroup args) {
		func_ = func;
		args_ = args;
	}
	
	private Expression func_;
	private ExpressionGroup args_;

}