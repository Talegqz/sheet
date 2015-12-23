package parser;

import java.util.ArrayList;

import ast.*;
import ast.symbol.ConstantSymbol;
import ast.symbol.FunctionSymbol;
import ast.symbol.Symbol;
import ast.symbol.VariableSymbol;
import lexer.*;
import scope.FunctionScope;
import scope.LocalScope;
import utils.LexicalError;
import utils.Position;
import utils.SyntaxError;

public class Parser {

	public Parser(Lexer lex) throws LexicalError {
		initialize(lex);
	}
	
	public Module parse() throws LexicalError, SyntaxError {
		parseModule();
		return module_;
	}

	private void initialize(Lexer lex) throws LexicalError {
		peek = null;
		lex_ = lex;
		
		astNodeFactory_ = new AstNodeFactory();
		module_ = new Module(this);
		scope_ = null;
		
		lowestBreakable = null;
		lowestIteration = null;
		
		advance();
	}

	private AstNodeFactory astNodeFactory_;
	/** The module object */
	private Module module_;
	/** Current local scope */
	private LocalScope scope_;
	
	private BreakableStatement lowestBreakable;
	private IterationStatement lowestIteration;

	// Lexical fields and functions

	private Token peek;
	private Lexer lex_;
	
	public Position position() {
		return lex_.position();
	}

	private Token expect(Tag wish) throws LexicalError, SyntaxError {
		if (peek.tag() == wish)
			return next();
		else
			throw new SyntaxError(position(), String.format(
					"expect %s instead of %s", wish.literal(), peek.literal()));
	}

	private void expectSemicolon() throws LexicalError, SyntaxError {
		expect(Tag.SEMICOLON);
	}

	private String expectIdentifier() throws LexicalError, SyntaxError {
		if (peek.isIdentifier())
			return (String) next().data();
		throw new SyntaxError(position(), String.format(
				"expect an identifier instead of %s", peek.literal()));
	}

	private boolean match(Tag tokenTag) throws LexicalError {
		if (peek.tag() == tokenTag) {
			advance();
			return true;
		}
		return false;
	}

	private Token next() throws LexicalError {
		Token save = peek;
		advance();
		return save;
	}

	private void advance() throws LexicalError {
		peek = lex_.advance();
	}

	// Parse a program in a file

	private void parseModule() throws LexicalError, SyntaxError {
		// Module ::
		//	ModuleDeclaration*
		scope_ = module_.scope();
		while (!match(Tag.EOS)) {
			parseModuleDeclaration();
		}
	}

	// Parse declarations.
	// Declarations will modify the context.
	
	/**
	 * Parse declarations in a module.
	 * Each sub-routine invoked in this routine will modify
	 * the current scope.
	 */
	private void parseModuleDeclaration()
			throws LexicalError, SyntaxError {
		// ModuleDeclaration ::
		//	ClassDeclaration |
		//	ConstantDeclaration |
		//	ExportDeclaration |
		//	ModuleFunctionDeclaration |
		//	ImportDeclaration |
		//	VariableDeclaration
		switch (peek.tag()) {
		case CLASS:
			throw new SyntaxError(position(), 
					"unimplemented parsing routine: class");
		case CONST:
			parseModuleConstantDeclaration();
			break;
		case EXPORT:
			throw new SyntaxError(position(), 
					"unimplemented parsing routine: export");
		case FUNCTION:
			parseModuleFunctionDeclaration();
			break;
		case IMPORT:
			throw new SyntaxError(position(), 
					"unimplemented parsing routine: import");
		case LET:
			parseModuleVariableDeclaration();
			break;
		default:
			throw new SyntaxError(position(), String.format(
					"error token %s, expect declarations", peek.literal()));
		}
	}
	
	// Constant declarations
	
	private void parseModuleConstantDeclaration()
			throws LexicalError, SyntaxError {
		ArrayList<Assignment> assigns = parseConstantDeclaration();
		module_.initializations().addAll(assigns);
	}
	
	private ExpressionStatement parseLocalConstantDeclaration()
			throws LexicalError, SyntaxError {
		ArrayList<Assignment> assigns = parseConstantDeclaration();
		ExpressionGroup eg = astNodeFactory_.newExpressionGroup(assigns);
		return astNodeFactory_.newExpressionStatement(eg);
	}
	
	private ArrayList<Assignment> parseConstantDeclaration()
			throws LexicalError, SyntaxError {
		ArrayList<Assignment> group = new ArrayList<Assignment>();
		expect(Tag.CONST);
		group.add(parseSingleConstantDeclaration());
		while (match(Tag.COMMA)) {
			group.add(parseSingleConstantDeclaration());
		}
		expectSemicolon();
		return group;
	}

	private Assignment parseSingleConstantDeclaration()
			throws LexicalError, SyntaxError {
		// SingleConstantDeclaration ::
		//	Identifier '=' Expression
		String id = expectIdentifier();
		expect(Tag.ASSIGN);
		Expression expr = parseExpression();
		ConstantSymbol symb = new ConstantSymbol(id);
		scope_.defineConstant(symb);
		return astNodeFactory_.newAssignment(Tag.INIT_CONST, symb.reference(),
				expr);
	}
	
	// Variable declarations
	
	private void parseModuleVariableDeclaration()
			throws LexicalError, SyntaxError {
		ArrayList<Assignment> assigns = parseVariableDeclaration();
		module_.initializations().addAll(assigns);
	}
	
	private ExpressionGroup parseLoopVariableDeclaration()
			throws LexicalError, SyntaxError {
		ArrayList<Assignment> assigns = parseVariableDeclaration();
		return astNodeFactory_.newExpressionGroup(assigns);
	}
	
	private ExpressionStatement parseLocalVariableDeclaration()
			throws LexicalError, SyntaxError {
		ArrayList<Assignment> assigns = parseVariableDeclaration();
		ExpressionGroup eg = astNodeFactory_.newExpressionGroup(assigns);
		return astNodeFactory_.newExpressionStatement(eg);
	}

	private ArrayList<Assignment> parseVariableDeclaration()
			throws LexicalError, SyntaxError {
		ArrayList<Assignment> group = new ArrayList<Assignment>();
		expect(Tag.LET);
		group.add(parseSingleVariableDeclaration());
		while (match(Tag.COMMA)) {
			group.add(parseSingleVariableDeclaration());
		}
		expectSemicolon();
		return group;
	}

	private Assignment parseSingleVariableDeclaration()
			throws LexicalError, SyntaxError {
		// SingleVariableDeclaration ::
		//	Identifier ('=' Expression)?
		String id = expectIdentifier();
		Expression expr = null;
		if (match(Tag.ASSIGN))
			expr = parseExpression();
		VariableSymbol symb = new VariableSymbol(id);
		scope_.defineVariable(symb);
		if (expr == null)
			return null;
		else
			return astNodeFactory_.newAssignment(Tag.INIT_LET, symb.reference(),
					expr);
	}

//	private void parseForEachVariableDeclaration(Scope loopScope)
//			throws LexicalError, SyntaxError {
//		// ForEachVariableDeclaration ::
//		//	'let' Identifier
//		expect(Tag.LET);
//		String id = expectIdentifier();
//		loopScope.defineVariable(id);
//	}

//	private void parseImportDeclaration() throws LexicalError, SyntaxError {
//		// ImportDeclaration ::
//		//	DirectlyImportDeclaration |
//		//	SelectiveImportDeclaration
//		// DirectlyImportDeclaration ::
//		//	'import' Identifier ';'
//		expect(Tag.IMPORT);
//
//	}
	
	private void parseModuleFunctionDeclaration()
			throws LexicalError, SyntaxError {
		FunctionSymbol symb = parseFunctionDeclaration();
		module_.scope().defineFunction(symb);
	}

	private FunctionSymbol parseFunctionDeclaration()
			throws LexicalError, SyntaxError {
		// FunctionDeclaration ::
		//	'function' Identifier '(' Arguments ')' FunctionBody
		
		// enter scope
		LocalScope origin = scope_;
		FunctionScope fscope = new FunctionScope(module_.scope());
		scope_ = fscope;
		
		expect(Tag.FUNCTION);
		String name = expectIdentifier();
		expect(Tag.LPAREN);
		if (!match(Tag.RPAREN))
			while (true) {
				String arg = expectIdentifier();
				VariableSymbol argSymb = new VariableSymbol(arg);
				fscope.defineArgument(argSymb);
				if (match(Tag.RPAREN))
					break;
				expect(Tag.COMMA);
			}
		StatementBlock funcBody = parseFunctionBody(fscope);
		
		// leave scope
		scope_ = origin;
		
		return new FunctionSymbol(name, fscope, funcBody);
	}

	private StatementBlock parseFunctionBody(FunctionScope fscope)
			throws LexicalError, SyntaxError {
		// FunctionBody ::
		//	'{' (Statement | VariableDeclaration | ConstantDeclaration)* '}'
		// Statement ::
		//	BreakStatement |
		//	ContinueStatement |
		//	DoWhileStatement |
		//	ExpressionStatement |
		//	ForStatement |
		//	ForEachStatement |
		//	IfStatement |
		//	ReturnStatement |
		//	StatementBlock |
		//	SwitchStatement |
		//	WhileStatement
		ArrayList<Statement> stmts = new ArrayList<Statement>();
		expect(Tag.LBRACE);
		while (!match(Tag.RBRACE)) {
			stmts.add(parseStatement());
		}
		return astNodeFactory_.newStatementBlock(stmts, fscope);
	}
	
	private Statement parseStatement() throws LexicalError, SyntaxError {
		switch (peek.tag()) {
		case BREAK:
			return parseBreakStatement();
		case CONST:
			return parseLocalConstantDeclaration();
		case CONTINUE:
			return parseContinueStatement();
		case DO:
			return parseDoWhileStatement();
		case EOS:
			throw new SyntaxError(position(), "unexpected end of source");
		case FOR:
			return parseForStatement();
		case FOREACH:
			throw new SyntaxError(position(), 
					"unimplemented parsing routine: foreach");
		case IF:
			return parseIfStatement();
		case LBRACE:
			return parseNakeStatementBlock();
		case LET:
			return parseLocalVariableDeclaration();
		case RETURN:
			return parseReturnStatement();
		case SWITCH:
			return parseSwitchStatement();
		case WHILE:
			return parseWhileStatement();
		default:
			return parseExpressionStatement();
		}
	}

	private Statement parseNakeStatementBlock()
			throws LexicalError, SyntaxError {
		// new scope
		LocalScope origin = scope_;
		scope_ = new LocalScope(origin);
		
		ArrayList<Statement> stmts = new ArrayList<Statement>();
		expect(Tag.LBRACE);
		while (!match(Tag.RBRACE)) {
			stmts.add(parseStatement());
		}
		
		// restore scope
		scope_ = origin;
		
		return astNodeFactory_.newStatementBlock(stmts, scope_);
	}

	private BreakStatement parseBreakStatement()
			throws LexicalError, SyntaxError {
		// BreakStatement ::
		//	'break' ';'
		if (lowestBreakable == null)
			throw new SyntaxError(position(),
					"break statement outside a breakable scope");
		expect(Tag.BREAK);
		expectSemicolon();
		return astNodeFactory_.newBreakStatement(lowestBreakable);
	}

	private ContinueStatement parseContinueStatement()
			throws LexicalError, SyntaxError {
		// ContinueStatement ::
		//	'continue' ';'
		if (lowestIteration == null)
			throw new SyntaxError(position(),
					"continue statement outside any iteration");
		expect(Tag.CONTINUE);
		expectSemicolon();
		return astNodeFactory_.newContinueStatement(lowestIteration);
	}

	private DoWhileStatement parseDoWhileStatement()
			throws LexicalError, SyntaxError {
		// DoWhileStatement ::
		//	'do' LoopBody 'while' '(' Expression ')' ';'
		DoWhileStatement loop = astNodeFactory_.newDoWhileStatement();
		
		// store
		IterationStatement saveIter = lowestIteration;
		BreakableStatement saveBreak = lowestBreakable;
		saveIter = loop;
		saveBreak = loop;
		
		expect(Tag.DO);
		Statement loopBody = parseStatement();
		expect(Tag.WHILE);
		Expression cond = parseParenthesisExpression();
		expectSemicolon();
		
		// restore
		lowestIteration = saveIter;
		lowestBreakable = saveBreak;
		
		loop.setup(cond, loopBody);
		return loop;
	}
	
	private ExpressionStatement parseExpressionStatement()
			throws LexicalError, SyntaxError {
		Expression expr = parseExpression();
		expectSemicolon();
		return astNodeFactory_.newExpressionStatement(expr);
	}

	private ForStatement parseForStatement() throws LexicalError, SyntaxError {
		// ForStatement ::
		//	'for' '(' (Expression | VariableDeclaration)
		//	';' Expression ';' Expression ')' LoopBody
		ForStatement loop = astNodeFactory_.newForStatement();
		
		// store
		IterationStatement saveIter = lowestIteration;
		BreakableStatement saveBreak = lowestBreakable;
		saveIter = loop;
		saveBreak = loop;
		
		// scope preparation
		LocalScope origin = scope_, lscope = new LocalScope(origin);
		scope_ = lscope;
		
		expect(Tag.FOR);
		expect(Tag.LPAREN);
		Expression initExpr = null;
		if (peek.tag() == Tag.LET) {
			initExpr = parseLoopVariableDeclaration();
		} else {
			initExpr = parseExpression();
			expectSemicolon();
		}
		Expression condExpr = parseExpression();
		expectSemicolon();
		Expression incrExpr = parseExpression();
		expect(Tag.RPAREN);
		Statement loopBody = parseStatement();
		
		// restore
		lowestIteration = saveIter;
		lowestBreakable = saveBreak;
		
		// scope clean up
		scope_ = origin;
		
		loop.setup(initExpr, condExpr, incrExpr, loopBody, lscope);
		return loop;
	}

//	private ForEachStatement parseForEachStatement()
//			throws LexicalError, SyntaxError {
//		enterScope();
//		expect(Tag.FOREACH);
//		expect(Tag.LPAREN);
//		<?> id = parseForEachVariableDeclaration();
//		expect(Tag.IN);
//		Expression expr = parseExpression();
//		expect(Tag.RPAREN);
//		Statement loopBody = parseStatement(false);
//		return astNodeFactory_.newForEachStatement(
//				id, expr, loopBody, exitScope());
//	}

	private IfStatement parseIfStatement() throws LexicalError, SyntaxError {
		// IfStatement ::
		//	'if' '(' Expression ')' Statement ('else' Statement)?
		expect(Tag.IF);
		Expression cond = parseParenthesisExpression();
		Statement then = parseStatement(), otherwise = null;
		if (match(Tag.ELSE)) {
			otherwise = parseStatement();
		}
		return astNodeFactory_.newIfStatement(cond, then, otherwise);
	}

	private ReturnStatement parseReturnStatement()
			throws LexicalError, SyntaxError {
		// ReturnStatement ::
		//	'return' Expression ';'
		expect(Tag.RETURN);
		Expression retExpr = null;
		if (!match(Tag.SEMICOLON)) {
			retExpr = parseExpression();
		}
		expectSemicolon();
		return astNodeFactory_.newReturnStatement(retExpr);
	}

	private Statement parseSwitchStatement() throws SyntaxError {
		throw new SyntaxError(position(),
				"unimplemented parsing routine: switch");
	}
	
	private WhileStatement parseWhileStatement()
			throws LexicalError, SyntaxError {
		// WhileStatement ::
		//	'while' '(' Expression ')' LoopBody
		WhileStatement loop = astNodeFactory_.newWhileStatement();
		// store
		IterationStatement saveIter = lowestIteration;
		BreakableStatement saveBreak = lowestBreakable;
		saveIter = loop;
		saveBreak = loop;
		
		expect(Tag.WHILE);
		Expression cond = parseParenthesisExpression();
		Statement body = parseStatement();
		
		// restore
		lowestIteration = saveIter;
		lowestBreakable = saveBreak;
		
		loop.setup(cond, body);
		return loop;
	}


	// Parse a variety of expressions.
	// We use top-down operator precedence method.
	// See further information here: 
	// http://javascript.crockford.com/tdop/tdop.html

	private Expression parseExpression() throws LexicalError, SyntaxError {
		return parseExpression(0);
	}

	private Expression parseExpression(int rbp)
			throws LexicalError, SyntaxError {
		Expression left = parseNullDenotation();
		while (rbp < peek.lbp()) {
			left = parseLeftDenotation(left);
		}
		return left;
	}

	private Expression parseNullDenotation() throws LexicalError, SyntaxError {
		switch (peek.tag()) {
		case CHAR_LITERAL:
		case STRING_LITERAL:
		case INTEGER:
		case NUMBER:
		case NULL_LITERAL:
		case TRUE_LITERAL:
		case FALSE_LITERAL:
			return astNodeFactory_.newLiteral(next());
		case LPAREN:
			return parseParenthesisExpression();
		case LBRACK:
			return parseArrayLiteral();
		case ADD:
		case SUB:
		case NOT:
		case BIT_NOT:
		case INC:
		case DEC:
			return parseUnaryOperation();
		case IDENTIFIER:
			return parseReference();
		default:
			throw new SyntaxError(position(), String.format(
					"error token %s: undefined value", peek.literal()));
		}
	}

	private Expression parseParenthesisExpression()
			throws LexicalError, SyntaxError {
		expect(Tag.LPAREN);
		Expression save = parseExpression();
		expect(Tag.RPAREN);
		return save;
	}

	private ArrayLiteral parseArrayLiteral() throws LexicalError, SyntaxError {
		// ArrayLiteral ::
		//	'[' (Expression (',' Expression)*)? ']'
		ArrayList<Expression> elems = new ArrayList<Expression>();
		expect(Tag.LBRACK);
		if (peek.tag() != Tag.RBRACK) {
			elems.add(parseExpression());
			while (match(Tag.COMMA))
				elems.add(parseExpression());
		}
		expect(Tag.RBRACK);
		return astNodeFactory_.newLiteral(elems);
	}

	private UnaryOperation parseUnaryOperation()
			throws LexicalError, SyntaxError {
		Token op = next();
		Expression expr = parseExpression(op.lbp());
		return astNodeFactory_.newUnaryOperation(op.tag(), expr);
	}
	
	private Reference parseReference() throws LexicalError, SyntaxError {
		// Reference :: Identifier
		String id = expectIdentifier();
		Symbol symb = scope_.find(id);
		return symb == null ?
				astNodeFactory_.newUnsolvedReference(id) : symb.reference();
	}

	private Expression parseLeftDenotation(Expression left)
			throws LexicalError, SyntaxError {
		switch (peek.tag()) {
		case CONDITIONAL:
			return parseConditional(left);
		case PERIOD:
			return parseProperty(left);
		case LPAREN:
			return parseInvoke(left);
		case LBRACK:
			return parseIndex(left);
		case INC:
		case DEC:
			return parsePostfixOperation(left);
		case COMMA:
			return parseExpressionGroup(left);
		case LT:
		case LTE:
		case GT:
		case GTE:
		case EQ:
		case NE:
			return parseCompareOperation(left);
		case BIT_AND:
		case BIT_OR:
		case BIT_XOR:
		case AND:
		case OR:
		case SHL:
		case SHR:
		case SAR:
		case ADD:
		case SUB:
		case MUL:
		case DIV:
		case MOD:
			return parseBianryOperation(left);
		case ASSIGN:
		case ASSIGN_BIT_OR:
		case ASSIGN_BIT_XOR:
		case ASSIGN_BIT_AND:
		case ASSIGN_SHL:
		case ASSIGN_SHR:
		case ASSIGN_SAR:
		case ASSIGN_ADD:
		case ASSIGN_SUB:
		case ASSIGN_MUL:
		case ASSIGN_DIV:
		case ASSIGN_MOD:
			return parseAssignment(left);
		default:
			throw new SyntaxError(position(), String.format(
					"error token %s, missing an operator", peek.literal()));
		}
	}

	private Expression parseInvoke(Expression left)
			throws LexicalError, SyntaxError {
		ExpressionGroup eg = null;
		expect(Tag.LPAREN);
		if (match(Tag.RPAREN)) {
			eg = new ExpressionGroup(new ArrayList<Expression>());
		} else {
			Expression first = parseExpression();
			if (match(Tag.COMMA)) {
				eg = parseExpressionGroup(first);
			} else {
				ArrayList<Expression> lst = new ArrayList<Expression>();
				lst.add(first);
				eg = astNodeFactory_.newExpressionGroup(lst);
			}
			expect(Tag.RPAREN);
		}
		return astNodeFactory_.newInvoke(left, eg);
	}

	private Conditional parseConditional(Expression cond)
			throws LexicalError, SyntaxError {
		expect(Tag.CONDITIONAL);
		Expression then = parseExpression();
		expect(Tag.COLON);
		Expression otherwise = parseExpression();
		return astNodeFactory_.newConditional(cond, then, otherwise);
	}

	private Property parseProperty(Expression left)
			throws LexicalError, SyntaxError {
		expect(Tag.PERIOD);
		String property = expectIdentifier();
		return astNodeFactory_.newProperty(left, property);
	}

	private Index parseIndex(Expression left)
			throws LexicalError, SyntaxError {
		expect(Tag.LBRACK);
		Expression index = parseExpression();
		expect(Tag.RBRACK);
		return astNodeFactory_.newIndex(left, index);
	}

	private UnaryOperation parsePostfixOperation(Expression left)
			throws LexicalError, SyntaxError {
		Token op = next();
		Tag postop = op.tag() == Tag.INC ? Tag.POSTFIX_INC : Tag.POSTFIX_DEC;
		return astNodeFactory_.newUnaryOperation(postop, left);
	}
	
	private ExpressionGroup parseExpressionGroup(Expression first)
			throws LexicalError, SyntaxError {
		ArrayList<Expression> group = new ArrayList<Expression>();
		Token comma = expect(Tag.COMMA);
		
		group.add(first);
		while (match(Tag.COMMA)) {
			group.add(parseExpression(comma.lbp()));
		}
		return astNodeFactory_.newExpressionGroup(group);
	}

	private CompareOperation parseCompareOperation(Expression left)
			throws LexicalError, SyntaxError {
		Token op = next();
		Expression right = parseExpression(op.lbp()); // left associative
		return astNodeFactory_.newCompareOperation(op.tag(), left, right);
	}

	private BinaryOperation parseBianryOperation(Expression left)
			 throws LexicalError, SyntaxError {
		Token op = next();
		Expression right = parseExpression(op.lbp()); // left associative
		return astNodeFactory_.newBinaryOperation(op.tag(), left, right);
	}

	private Assignment parseAssignment(Expression left)
			throws LexicalError, SyntaxError {
		Token op = next();
		Expression right = parseExpression(op.lbp() - 1); // right associative
		return astNodeFactory_.newAssignment(op.tag(), left, right);
	}

}