package runtime;

import java.util.Stack;

public class StackFrame {
	
	public StackFrame() {
		this(null, 0, null);
	}

	public StackFrame(StackFrame prev, int localCount, RuntimeObject[] args) {
		previous_ = prev;
		stack_ = new Stack<RuntimeObject>();
		locals_ = localCount == 0 ? null : new RuntimeObject[localCount];
		args_ = args;
	}
	
	private StackFrame previous_;
	private Stack<RuntimeObject> stack_;
	private RuntimeObject[] locals_;
	private RuntimeObject[] args_;
	
	public void push(RuntimeObject obj) {
		stack_.push(obj);
	}
	
	public RuntimeObject pop() {
		return stack_.pop();
	}
	
	public boolean empty() {
		return stack_.isEmpty();
	}
	
	public RuntimeObject[] arguments() {
		return args_;
	}
	
	public RuntimeObject[] locals() {
		return locals_;
	}

}
