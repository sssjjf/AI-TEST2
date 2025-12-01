package com.example.test;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.*;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

public class JavaAstToMermaid {
    
    public static void main(String[] args) throws Exception {
        List<ClassInfo> classInfos = new ArrayList<>();
        
        // 遍历Java文件
        File projectDir = new File("D:\\(1)IDEA\\IDEA_project\\jpatest\\src\\main\\java\\com\\example\\jpatest");
        processDirectory(projectDir, classInfos);
        
        // 生成Mermaid图
        String mermaid = generateMermaid(classInfos);
        
        // 保存到文件
        saveToFile(mermaid, "class-diagram.md");
        
        // 也可以直接输出
        System.out.println(mermaid);
    }
    
    private static void processDirectory(File dir, List<ClassInfo> classInfos) throws Exception {
        if (dir.isDirectory()) {
            for (File file : dir.listFiles()) {
                if (file.isDirectory()) {
                    processDirectory(file, classInfos);
                } else if (file.getName().endsWith(".java")) {
                    processJavaFile(file, classInfos);
                }
            }
        }
    }
    
    private static void processJavaFile(File javaFile, List<ClassInfo> classInfos) throws Exception {
        JavaParser parser = new JavaParser();
        CompilationUnit cu = parser.parse(javaFile).getResult().orElseThrow(()->new Exception("xxx"));
        
        cu.findAll(ClassOrInterfaceDeclaration.class).forEach(classDecl -> {
            ClassInfo classInfo = new ClassInfo();
            classInfo.className = classDecl.getNameAsString();
            classInfo.packageName = cu.getPackageDeclaration()
                .map(pd -> pd.getNameAsString())
                .orElse("");
            
            // 获取继承关系
            classDecl.getExtendedTypes().forEach(ext -> {
                classInfo.extendsClasses.add(ext.getNameAsString());
            });
            
            // 获取实现关系
            classDecl.getImplementedTypes().forEach(impl -> {
                classInfo.implementsInterfaces.add(impl.getNameAsString());
            });
            
            // 获取字段
            classDecl.getFields().forEach(field -> {
                String fieldStr = field.getVariables().get(0).getNameAsString() + 
                                " : " + field.getElementType().asString();
                classInfo.fields.add(fieldStr);
            });
            
            // 获取方法
            classDecl.getMethods().forEach(method -> {
                String methodStr = method.getNameAsString() + "()";
                classInfo.methods.add(methodStr);
            });
            
            // 分析依赖关系（简化版）
            analyzeDependencies(classDecl, classInfo);
            
            classInfos.add(classInfo);
        });
    }
    
    private static void analyzeDependencies(ClassOrInterfaceDeclaration classDecl, ClassInfo classInfo) {
        // 分析使用的其他类（通过类型引用）
        classDecl.findAll(com.github.javaparser.ast.type.Type.class).forEach(type -> {
            if (type.isClassOrInterfaceType()) {
                String typeName = type.asClassOrInterfaceType().getNameAsString();
                if (!typeName.equals(classInfo.className)) {
                    classInfo.dependencies.put(typeName, "uses");
                }
            }
        });
        
        // 分析构造函数中的依赖
        classDecl.findAll(com.github.javaparser.ast.expr.ObjectCreationExpr.class).forEach(creation -> {
            creation.getType().ifPrimitiveType(type -> {
                String typeName = type.asString();
                classInfo.dependencies.put(typeName, "creates");
            });
        });
    }
    
    private static String generateMermaid(List<ClassInfo> classInfos) {
        StringBuilder mermaid = new StringBuilder();
        mermaid.append("```mermaid\n");
        mermaid.append("classDiagram\n");
        
        // 1. 定义所有类
        for (ClassInfo info : classInfos) {
            mermaid.append("    class ").append(info.className).append(" {\n");
            
            // 添加字段
            for (String field : info.fields) {
                mermaid.append("        ").append(field).append("\n");
            }
            
            // 添加方法
            if (!info.methods.isEmpty()) {
                mermaid.append("        ").append("---方法---").append("\n");
                for (String method : info.methods) {
                    mermaid.append("        ").append(method).append("\n");
                }
            }
            
            mermaid.append("    }\n");
        }
        
        // 2. 添加继承和实现关系
        for (ClassInfo info : classInfos) {
            for (String parent : info.extendsClasses) {
                mermaid.append("    ").append(parent)
                       .append(" <|-- ").append(info.className)
                       .append(" : 继承\n");
            }
            
            for (String intf : info.implementsInterfaces) {
                mermaid.append("    ").append(intf)
                       .append(" <|.. ").append(info.className)
                       .append(" : 实现\n");
            }
        }
        
        // 3. 添加依赖关系（可选，避免图太乱）
        mermaid.append("\n    %% 依赖关系\n");
        for (ClassInfo info : classInfos) {
            for (Map.Entry<String, String> dep : info.dependencies.entrySet()) {
                // 只显示目标类也在图中的依赖
                if (classInfos.stream().anyMatch(c -> c.className.equals(dep.getKey()))) {
                    mermaid.append("    ").append(info.className)
                           .append(" --> ").append(dep.getKey())
                           .append(" : ").append(dep.getValue()).append("\n");
                }
            }
        }
        
        mermaid.append("```\n");
        return mermaid.toString();
    }
    
    private static void saveToFile(String content, String filename) {
        try (FileWriter writer = new FileWriter(filename)) {
            writer.write(content);
            System.out.println("Mermaid图已保存到: " + filename);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    static class ClassInfo {
        String className;
        String packageName;
        List<String> fields = new ArrayList<>();
        List<String> methods = new ArrayList<>();
        List<String> extendsClasses = new ArrayList<>();
        List<String> implementsInterfaces = new ArrayList<>();
        Map<String, String> dependencies = new HashMap<>();
    }
}