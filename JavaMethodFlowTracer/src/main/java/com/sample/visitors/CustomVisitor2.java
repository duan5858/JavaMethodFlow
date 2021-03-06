/*
 * � 2009-2015 T-Systems International GmbH. All rights reserved
 * _______________UTF-8 checked via this umlaut: �
 */
package com.sample.visitors;

import japa.parser.ast.body.MethodDeclaration;
import japa.parser.ast.expr.ArrayAccessExpr;
import japa.parser.ast.expr.AssignExpr;
import japa.parser.ast.expr.BooleanLiteralExpr;
import japa.parser.ast.expr.CastExpr;
import japa.parser.ast.expr.CharLiteralExpr;
import japa.parser.ast.expr.ConditionalExpr;
import japa.parser.ast.expr.DoubleLiteralExpr;
import japa.parser.ast.expr.Expression;
import japa.parser.ast.expr.FieldAccessExpr;
import japa.parser.ast.expr.IntegerLiteralExpr;
import japa.parser.ast.expr.LongLiteralExpr;
import japa.parser.ast.expr.MethodCallExpr;
import japa.parser.ast.expr.NameExpr;
import japa.parser.ast.expr.NullLiteralExpr;
import japa.parser.ast.expr.ObjectCreationExpr;
import japa.parser.ast.expr.StringLiteralExpr;
import japa.parser.ast.expr.VariableDeclarationExpr;
import japa.parser.ast.visitor.VoidVisitorAdapter;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.sample.base.ClassOrInterfaceStruct2;
import com.sample.base.MethodCallStruct2;
import com.sample.base.MethodStruct2;
import com.sample.base.VariableStruct2;
import com.sample.containers.ClassStructContainer2;
import com.sample.utils.MethodUtils;

/**
 * TODO Small and simple description of the type
 *
 * @copyright � 2009-2015 T-Systems International GmbH. All rights reserved
 * @author 122305
 * 
 * @changes 
 *    May 7, 2015: Created
 *
 */
public class CustomVisitor2 extends VoidVisitorAdapter {
	
	private MethodSigVisitor methodSigVisitor = new MethodSigVisitor();
	
	
	public CustomVisitor2() {
		
	}
	
	@Override
	public void visit(ObjectCreationExpr n, Object arg) {
		AssignExpr assignExpr = (AssignExpr) methodSigVisitor.getParentOfType(n, AssignExpr.class);

		if(assignExpr != null) {
			ClassOrInterfaceStruct2 parent = methodSigVisitor.getClassOrInterfaceStruct(assignExpr);
			MethodDeclaration methodDec = (MethodDeclaration) methodSigVisitor.getParentOfType(assignExpr, MethodDeclaration.class);
			
			if(methodDec != null) {
				MethodStruct2 m_struct = methodSigVisitor.getMethodStruct(methodDec, parent);
				evaluateAssignExpr(assignExpr, parent, m_struct);
			}
		}
	}
	
	public void evaluateAssignExpr(AssignExpr a_expr, ClassOrInterfaceStruct2 parent, 
			MethodStruct2 m_struct) {
		if(a_expr.getValue() instanceof ObjectCreationExpr) {
			
			String methodVarName = null;
			
			if(a_expr.getTarget() instanceof NameExpr) {
				methodVarName = ((NameExpr)a_expr.getTarget()).getName();
			} else if(a_expr.getTarget() instanceof ArrayAccessExpr) {
				ArrayAccessExpr arr_acc_expr = (ArrayAccessExpr) a_expr.getTarget();
				methodVarName = ((NameExpr)arr_acc_expr.getName()).getName();
			}
			
			if(methodVarName != null) {

				//Search at method
				//Check for variable at class level. TODO method level and method call args check.
				VariableStruct2 varStruct =  this.methodSigVisitor.searchForVariable(methodVarName, parent, m_struct, m_struct.getVars(), false);
				
				if(varStruct != null) {
					ObjectCreationExpr obj_expr = (ObjectCreationExpr) a_expr.getValue();
					varStruct.setValueTypeQualified(methodSigVisitor.processObjecInitExpr(obj_expr, parent));	
				}
			}
		}		
	}
	
	@Override
	public void visit(VariableDeclarationExpr n, Object arg)
	{
		super.visit(n, arg);
		ClassOrInterfaceStruct2 parent = methodSigVisitor.getClassOrInterfaceStruct(n);

		//Get the method that contains this variable dec (if this is local to method) if present.
		MethodDeclaration methodDec = (MethodDeclaration) methodSigVisitor.getParentOfType(n, MethodDeclaration.class);
		
		if(methodDec != null) {
			
			//This variable is declared within a method.
			MethodStruct2 m_struct = methodSigVisitor.getMethodStruct(methodDec, parent);
			VariableStruct2 v_struct = evaluateVariableDeclExpr(n, parent, m_struct);
			m_struct.getVars().put(v_struct.getName(), v_struct);			
		}
	}
	

	@Override
	public void visit(MethodCallExpr n, Object arg)
	{
		ClassOrInterfaceStruct2 parent = methodSigVisitor.getClassOrInterfaceStruct(n);
		MethodDeclaration methodDec = (MethodDeclaration) methodSigVisitor.getParentOfType(n, MethodDeclaration.class);
		
		if(methodDec != null) {
			MethodStruct2 m_struct = methodSigVisitor.getMethodStruct(methodDec, parent);
			evaluateMethodCallExpr(n, parent, m_struct, m_struct.getVars());
		}
		super.visit(n, arg);
	
	}

	public VariableStruct2 evaluateVariableDeclExpr(VariableDeclarationExpr varExpr, ClassOrInterfaceStruct2 parent, 
			MethodStruct2 m_struct) {
		VariableStruct2 var  = methodSigVisitor.evaluateType(varExpr.getType(), parent);
		var.setName(varExpr.getVars().get(0).getId().toString());
		var.setParentMethod(m_struct); 
		Expression expr =  varExpr.getVars().get(0).getInit();
		
		//if variable is initilised with new keywork then get the actual type as well.
		if(expr != null) {
			
			if(expr instanceof ObjectCreationExpr) {
				ObjectCreationExpr obj_expr = (ObjectCreationExpr) expr;
				var.setValueTypeQualified(methodSigVisitor.processObjecInitExpr(obj_expr, parent));
			}
		}
		return var;
	}

	/*
	 *
	 ****** Case -1 : method var. ex: ******
		 	sayHello{
				Rest rest = new Rest();
				rest.printbye();
		   	}

			VariableStruct 
	 			pkg = com.sample
	 			clazz = Rest
	 			method = sayHello
	 			var = rest

	   		calledmethodQname
	 			pkg = com.sample
	 			clazz = Rest
	 			method = printBye

	 ****** case-2: instance var. eg:  ******

	 		class Test {
	 			Rest rest = new Rest();
				 sayHello{					
					rest.printbye();
				   }	 		
	 		}

			VariableStruct 
	 			pkg = com.sample
	 			clazz = Rest
	 			var = rest

	   		calledmethodQname
	 			pkg = com.sample
	 			clazz = Rest
	 			method = printBye

	 ****** case-3: static var. eg:  ******

	 		class Test {
	 			import com.sample.Rest;

				 sayHello{					
					Rest.printbye();
				   }	 		
	 		}

			VariableStruct 
	 			pkg = com.sample
	 			clazz = Rest

	   		calledmethodQname
	 			pkg = com.sample
	 			clazz = Rest
	 			method = printBye


	 */
	
	private static Map<Class, String> exprTypeMap = new HashMap<Class, String>();
	static {
		exprTypeMap.put(LongLiteralExpr.class, "long");
		exprTypeMap.put(CharLiteralExpr.class, "char");
		exprTypeMap.put(IntegerLiteralExpr.class, "int");
		exprTypeMap.put(DoubleLiteralExpr.class, "double");
		exprTypeMap.put(StringLiteralExpr.class, "java.lang.String");
		exprTypeMap.put(BooleanLiteralExpr.class, "boolean");
		exprTypeMap.put(NullLiteralExpr.class, null);

	}
	
	private List<String> parseMethodCallArgs(MethodCallExpr mcall_expr, ClassOrInterfaceStruct2 parent, MethodStruct2 m_struct) {
		List<Expression> args = mcall_expr.getArgs();
		List<String> argsType = new LinkedList<String>();
		
		if(args != null && !args.isEmpty()) {
			for(Expression arg : args) {
				argsType.add(getQNameForExpr(arg, parent, m_struct, false));
			}
		}
		return argsType;
	}
	
	public String getQNameForExpr(Expression expression, ClassOrInterfaceStruct2 parent, MethodStruct2 m_struct, boolean resolveInterface) {
		String qtype = exprTypeMap.get(expression.getClass());
		
		if(qtype == null) {
			if(expression instanceof MethodCallExpr) {
				//TODO:
				MethodCallExpr mcall_expr = (MethodCallExpr) expression;
				String mcallQnameWithoutArgs = getQNameForMethodCallExpr(mcall_expr, parent, m_struct, false);
				
				List<String> argsType = parseMethodCallArgs(mcall_expr, parent, m_struct);
				MethodStruct2 m_struct_child = ClassStructContainer2.getInstance().getMatchingMethod(mcallQnameWithoutArgs, argsType, null, null);
				
				if(m_struct_child != null) {
					qtype = m_struct_child.getReturnType();
				}				
			} else if(expression instanceof CastExpr) {
				CastExpr cast_expr = (CastExpr)expression;
				qtype = methodSigVisitor.evaluateType(cast_expr.getType(), parent).toString();			
			} else if(expression instanceof ConditionalExpr) {
				ConditionalExpr cond_expr = (ConditionalExpr) expression;
				return getQNameForExpr(cond_expr.getThenExpr(), parent, m_struct, resolveInterface);
			} else if(expression instanceof FieldAccessExpr) {
				FieldAccessExpr f_expr = (FieldAccessExpr) expression;
				Expression f_scope_expr = f_expr.getScope();
				String fieldName = f_expr.getField();
				
				if(f_scope_expr != null) {
					String f_scope_name = getQNameForExpr(f_scope_expr, parent, m_struct, resolveInterface);
					ClassOrInterfaceStruct2 scopeType = ClassStructContainer2.getInstance().getClassOrInterface(f_scope_name);
					
					if(scopeType == null) {
						//Class Container does not hold the class. It could be a in java itself or in one of libraries.
						qtype = f_scope_name + "." + fieldName;
					} else {
						
						//search for the field in the class scopeType.
						qtype = getQNameForExpr(f_expr.getFieldExpr(), scopeType, m_struct, resolveInterface);
					}
				}
				
			} else if(expression instanceof ArrayAccessExpr) {
				ArrayAccessExpr arr_acc_expr = (ArrayAccessExpr) expression;
				return getQNameForExpr(arr_acc_expr.getName(), parent, m_struct, resolveInterface);
			} else if(expression instanceof NameExpr) {
				NameExpr n_expr = (NameExpr) expression;
				VariableStruct2 varStruct =  this.methodSigVisitor.searchForVariable(n_expr.getName(), parent, m_struct, m_struct.getVars(), true);
				
				if(varStruct != null) {
					
					if(resolveInterface && varStruct.getValueTypeQualified() != null) {
						qtype = varStruct.getValueTypeQualified();
					} else {
						qtype = varStruct.getQualifiedNameWithoutVarName();	
					}
					
				}			
			} else if(expression instanceof ObjectCreationExpr) {
				qtype = methodSigVisitor.processObjecInitExpr((ObjectCreationExpr)expression, parent);
			}
		}

		//TODO: handle other type of expressions.
		//TODO: handle arrays.
		return qtype;
	}
	
	protected String getQNameForMethodCallExpr(MethodCallExpr mexpr, ClassOrInterfaceStruct2 parent, 
			MethodStruct2 m_struct, boolean appendArgs) {
		String calledMethodQname = "";
		String calledMethodName = mexpr.getName();
		
		if(mexpr.getScope() == null || "this".equals(mexpr.getScope())) {
			//scope is null which means the source and target methods are availble in same class. so use pkg, clazz values of the parent source method itself.
			calledMethodQname = m_struct.getPkg() + "." + m_struct.getClazz() + "." + calledMethodName;
		} else if("super".equals(mexpr.getScope())) {
			String superClazzQName = m_struct.getParent().getSuperClasses().get(0);
			calledMethodQname = superClazzQName + "." + calledMethodName;
		} else {
			//TODO: handle protected/public variables of parent.
			//search for method variable declaration to find the target method types. The target method variable declaration can be either at method level, or at   
			//class object level or class level.
			Expression scope_Expr = mexpr.getScope();
			System.out.println(scope_Expr);
			calledMethodQname = getQNameForExpr(scope_Expr, parent, m_struct, true) + "." + calledMethodName;
		}
		
		if(appendArgs) {
			String argsCSString = MethodUtils.qnameCollToCSSepString(parseMethodCallArgs(mexpr, parent, m_struct));
			
			if(argsCSString != null) {
				calledMethodQname = calledMethodQname + "<" + argsCSString + ">";
			}
		}
		
		return calledMethodQname;
	}
	
	public MethodCallStruct2 evaluateMethodCallExpr(MethodCallExpr mexpr, ClassOrInterfaceStruct2 parent, 
			MethodStruct2 m_struct, Map<String, VariableStruct2> methodVariables) {
		MethodCallStruct2 callStruct = new MethodCallStruct2();
		String calledMethodQname = getQNameForMethodCallExpr(mexpr, parent, m_struct, false);
		callStruct.setCalledMethod(calledMethodQname);

		//TODO: evaluate method args and add to MethodCallStruct.
		callStruct.setCallArgs(parseMethodCallArgs(mexpr, parent, m_struct));
		
		if(!m_struct.getCalledMethods().contains(callStruct)) {
			m_struct.getCalledMethods().add(callStruct);	
		}		

		return callStruct;

	}

}
