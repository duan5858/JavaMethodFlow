/*
 * � 2009-2015 T-Systems International GmbH. All rights reserved
 * _______________UTF-8 checked via this umlaut: �
 */
package com.sample.visitors;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.sample.base.ClassOrInterfaceStruct2;
import com.sample.base.ClassType;
import com.sample.base.MethodStruct2;
import com.sample.base.VariableStruct2;
import com.sample.containers.ClassStructContainer2;
import com.sample.utils.LangUtils;

import japa.parser.ast.CompilationUnit;
import japa.parser.ast.ImportDeclaration;
import japa.parser.ast.Node;
import japa.parser.ast.body.ClassOrInterfaceDeclaration;
import japa.parser.ast.body.FieldDeclaration;
import japa.parser.ast.body.MethodDeclaration;
import japa.parser.ast.body.Parameter;
import japa.parser.ast.expr.Expression;
import japa.parser.ast.expr.ObjectCreationExpr;
import japa.parser.ast.type.ClassOrInterfaceType;
import japa.parser.ast.type.PrimitiveType;
import japa.parser.ast.type.Type;
import japa.parser.ast.type.VoidType;
import japa.parser.ast.visitor.VoidVisitorAdapter;

/**
 * TODO Small and simple description of the type
 *
 * @copyright � 2009-2015 T-Systems International GmbH. All rights reserved
 * @author 122305
 * 
 * @changes 
 *    May 20, 2015: Created
 *
 */
public class MethodSigVisitor extends VoidVisitorAdapter {

	public Node getParentOfType(Node currentNode, Class parentType) {

		if(currentNode.getParentNode() != null) {

			if(parentType.isAssignableFrom(currentNode.getParentNode().getClass())) {
				return currentNode.getParentNode();
			} else {
				return getParentOfType(currentNode.getParentNode(), parentType);
			}
		}
		return null;
	}


	public ClassOrInterfaceStruct2 getClassOrInterfaceStruct(Node n) {
		ClassOrInterfaceDeclaration clazzDec = (ClassOrInterfaceDeclaration) getParentOfType(n, ClassOrInterfaceDeclaration.class);
		CompilationUnit cu = (CompilationUnit) getParentOfType(clazzDec, CompilationUnit.class);
		String pkg = cu.getPackage().getName().toString();

		//Get the class struct to which this field should be added.
		String key = pkg + "." + clazzDec.getName();
		ClassOrInterfaceStruct2 parent = ClassStructContainer2.getInstance().getClasses().get(key);
		return parent;
	}	

	
	public MethodStruct2 getMethodStruct(MethodDeclaration n, ClassOrInterfaceStruct2 parent) {		
		String m_name = n.getName();
		StringBuffer buff = new StringBuffer();
		buff.append(parent.getQualifiedName()).append(".").append(m_name);
		Map<String, VariableStruct2> paramMap = evaluateMethodVariables(n.getParameters(), parent, null);
		
		if(paramMap != null && !paramMap.isEmpty()) {
			buff.append("<");
			int i = 1;
			for(VariableStruct2 var : paramMap.values()) {
				if(var.getTypePkg() != null) {
					buff.append(var.getTypePkg()).append(".");
				}
				if(var.getType() != null) {
					buff.append(var.getType());	
				}				
				if(i < paramMap.size()) {
					buff.append(",");
				}		
				i++;
			}
			buff.append(">");			
		}
		String methodQName = buff.toString();
		
		if(parent.getMethods().containsKey(methodQName)) {
			return parent.getMethods().get(methodQName);
		}
		return null;
	}
	
	@Override
	public void visit(ClassOrInterfaceDeclaration n, Object arg)
	{

		ClassOrInterfaceStruct2 clazzStructInst = new ClassOrInterfaceStruct2();
		CompilationUnit cu = (CompilationUnit) getParentOfType(n, CompilationUnit.class);
		if ( cu != null && cu.getImports() != null ) {
			for ( ImportDeclaration i : cu.getImports() ) {
				if(i.isAsterisk()) {
					clazzStructInst.getImports().putAll(LangUtils.resolveAsterickImport(i.getName().toString()));
				} else {
					clazzStructInst.getImports().put(i.getName().getName(), i.getName().toString());	
				}				
				//TODO: handle static imports.
			}
		}

		clazzStructInst.setName(n.getName());
		clazzStructInst.setPkg(cu.getPackage().getName().toString());

		if(n.isInterface()) {
			clazzStructInst.setType(ClassType.INTERFACE);
		} else {
			clazzStructInst.setType(ClassType.CLASS);
		}
		
		if ( n.getExtends() != null ) {
			for ( ClassOrInterfaceType c : n.getExtends() ) {
				String superClassStr = c.getName();

				//Get Fully qualified name.
				String supperClassQualified = clazzStructInst.getImports().get(superClassStr);

				if(supperClassQualified == null) {
					//Default package handle.
					supperClassQualified = clazzStructInst.getQualifiedName();
				}
				clazzStructInst.getSuperClasses().put(superClassStr, supperClassQualified);
			}
		}

		if ( n.getImplements() != null ) {
			for ( ClassOrInterfaceType c : n.getImplements() ) {
				String classStr = c.getName();

				//Get Fully qualified name.
				String classQualified = clazzStructInst.getImports().get(classStr);

				if(classQualified == null) {
					//Default package handle.
					classQualified = clazzStructInst.getQualifiedName();
				}
				clazzStructInst.getInterfacesImplemented().put(classStr, classQualified);
			}
		}
		ClassStructContainer2.getInstance().getClasses().put(clazzStructInst.getQualifiedName(), clazzStructInst);
		super.visit(n, arg);
	}

	
	@Override
	public void visit(FieldDeclaration n, Object arg) {
		ClassOrInterfaceStruct2 parent = getClassOrInterfaceStruct(n);
		String fieldType = n.getType().toString();
		String fieldTypeQualified = parent.getImports().get(fieldType);
		VariableStruct2 var = new VariableStruct2();

		if(fieldTypeQualified == null) {

			try {
				//Check if field type is primitive like int,boolaen etc.. if so ignore qualification.
				if(!LangUtils.isPrimitiveType(fieldType)) {
					//Check if the class belong to java.lang package
					if(LangUtils.isClassFromLangPackage(fieldType)) {
						fieldTypeQualified = "java.lang." + fieldType;
					}  else {
						//Default package.
						//TODO: handle inline package decl
						fieldTypeQualified = parent.getPkg() + "." + fieldType;
					}
				}
			}
			catch ( Exception e ) {
				e.printStackTrace();
			}

		}

		if(fieldTypeQualified != null) {
			var.setTypePkg(fieldTypeQualified.substring(0, fieldTypeQualified.lastIndexOf(".")));	
		}		
		var.setType(fieldType);		
		var.setName(n.getVariables().get(0).getId().toString());
		var.setParent(parent);
		Expression expr =  n.getVariables().get(0).getInit();
		
		//if variable is initilised with new keywork then get the actual type as well.
		if(expr != null) {
			
			if(expr instanceof ObjectCreationExpr) {
				ObjectCreationExpr obj_expr = (ObjectCreationExpr) expr;
				var.setValueTypeQualified(processObjecInitExpr(obj_expr, parent));
			}
		}
		
		parent.getVariables().put(var.getName(), var);
		super.visit(n, arg);
	}

	public String processObjecInitExpr(ObjectCreationExpr obj_expr, ClassOrInterfaceStruct2 parent) {
		String classStr =  obj_expr.getType().getName();
		String classQualified = parent.getImports().get(classStr);
		String pkg = null;
		
		if(classQualified == null) {
			//Default package handle.
			pkg = parent.getPkg();
		} else {
			pkg = classQualified.substring(0, classQualified.lastIndexOf("."));
		}
		return pkg + "." + classStr;
	}

	@Override
	public void visit(MethodDeclaration n, Object arg) {
		ClassOrInterfaceStruct2 parent = getClassOrInterfaceStruct(n);
		MethodStruct2 m_struct = new MethodStruct2();
		m_struct.setName(n.getName());
		m_struct.setClazz(parent.getName());
		m_struct.setPkg(parent.getPkg());
		m_struct.setParent(parent);

		//Extract method call arguments and store in method.
		Map<String, VariableStruct2> m_vars = evaluateMethodVariables(n.getParameters(), parent, m_struct);
		if(m_vars != null && !m_vars.isEmpty()) {
			m_struct.getCallArgs().putAll(m_vars);	
		}
		VariableStruct2 returnType = evaluateType(n.getType(), parent);
		
		if(returnType != null) {
			m_struct.setReturnType(returnType.getQualifiedName());	
		}
		
		//evaluateMethodBody(n.getBody(), parent, m_struct);		
		parent.getMethods().put(m_struct.getQualifiedNameWithArgs(), m_struct);
	}
	
	public Map<String, VariableStruct2> evaluateMethodVariables(List<Parameter> methodCallArgs, ClassOrInterfaceStruct2 parent, 
			MethodStruct2 m_struct) {
		if(methodCallArgs != null && !methodCallArgs.isEmpty()) {
			Map<String, VariableStruct2> params = new LinkedHashMap<String, VariableStruct2>();

			for(Parameter methodCallArg : methodCallArgs) {
				VariableStruct2 var = evaluateType(methodCallArg.getType(), parent);
				var.setName(methodCallArg.getId().getName());
				var.setParentMethod(m_struct);
				params.put(var.getName(), var);				 
			}
			return params;
		}
		return null;		
	}
	
	
	public VariableStruct2 evaluateType(Type type, ClassOrInterfaceStruct2 parent) {
		VariableStruct2 var = new VariableStruct2();
		String fieldType = type.toString();
		String fieldTypeQualified = parent.getImports().get(fieldType);
		if(fieldTypeQualified == null) {
			try {
				if(type instanceof VoidType) {
					return null;
				}
				
				//Check if field type is primitive like int,boolaen etc.. if so ignore qualification.
				if(!(type instanceof PrimitiveType)) {
					
					//Check if the class belong to java.lang package
					if(LangUtils.isClassFromLangPackage(fieldType)) {
						fieldTypeQualified = "java.lang." + fieldType;
					}  else {
						//Default package.
						//TODO: handle inline package decl
						fieldTypeQualified = parent.getPkg() + "." + fieldType;
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		if(fieldTypeQualified != null) {
			var.setTypePkg(fieldTypeQualified.substring(0, fieldTypeQualified.lastIndexOf(".")));	
		}		
		var.setType(fieldType);		
		return var;
	}	
}
