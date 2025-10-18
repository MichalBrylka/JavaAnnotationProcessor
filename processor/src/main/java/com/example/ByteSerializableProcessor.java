// src/main/java/com/example/processor/ByteSerializableProcessor.java
package com.example;

import com.example.annotations.ByteSerializable;
import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.*;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@SupportedAnnotationTypes("com.example.annotations.ByteSerializable")
@SupportedSourceVersion(SourceVersion.RELEASE_17) // Use your Java version
public class ByteSerializableProcessor extends AbstractProcessor {

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        System.out.println("DUPAXXXXXXXXXXXXXXXXXXXXXXXXXXXXX");
        for (Element element : roundEnv.getElementsAnnotatedWith(ByteSerializable.class)) {
            if (element.getKind() == ElementKind.CLASS) {
                TypeElement typeElement = (TypeElement) element;
                try {
                    generateSerializer(typeElement);
                } catch (IOException e) {
                    processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "Failed to generate serializer for " + typeElement.getQualifiedName() + ": " + e.getMessage());
                }
            }
        }
        return true;
    }

    private void generateSerializer(TypeElement typeElement) throws IOException {
        String packageName = processingEnv.getElementUtils().getPackageOf(typeElement).getQualifiedName().toString();
        String className = typeElement.getSimpleName().toString();
        String serializerClassName = className + "Serializer";
        String fullSerializerClassName = packageName.isEmpty() ? serializerClassName : packageName + "." + serializerClassName;

        // Create a new source file
        JavaFileObject builderFile = processingEnv.getFiler().createSourceFile(fullSerializerClassName);

        try (PrintWriter out = new PrintWriter(builderFile.openWriter())) {
            writeClassHeader(out, packageName, className, serializerClassName);

            // Get fields (assuming Lombok @Value creates fields matching constructor arguments)
            List<VariableElement> fields = getRelevantFields(typeElement);

            // Serialization method
            writeSerializationMethod(out, className, fields);

            // Deserialization method
            writeDeserializationMethod(out, className, fields);

            writeClassFooter(out);
        }
    }

    private List<VariableElement> getRelevantFields(TypeElement typeElement) {
        // Collect all non-static fields declared in the class
        return typeElement.getEnclosedElements().stream()
                .filter(e -> e.getKind() == ElementKind.FIELD)
                .map(e -> (VariableElement) e)
                .collect(Collectors.toList());
    }

    private void writeClassHeader(PrintWriter out, String packageName, String className, String serializerClassName) {
        if (!packageName.isEmpty()) {
            out.println("package " + packageName + ";");
            out.println();
        }
        out.println("import java.io.*;");
        out.println("import java.time.LocalDate;");
        out.println("import java.util.ArrayList;");
        out.println("import java.util.List;");
        out.println();
        out.println("public class " + serializerClassName + " {");
    }

    private void writeClassFooter(PrintWriter out) {
        out.println("}");
    }

    private void writeSerializationMethod(PrintWriter out, String className, List<VariableElement> fields) {
        out.println();
        out.println("    public static byte[] serialize(" + className + " obj) throws IOException {");
        out.println("        try (ByteArrayOutputStream bos = new ByteArrayOutputStream();");
        out.println("             DataOutputStream dos = new DataOutputStream(bos)) {");

        for (VariableElement field : fields) {
            String fieldName = field.getSimpleName().toString();
            String typeName = field.asType().toString();
            String getter = "obj.get" + fieldName.substring(0, 1).toUpperCase() + fieldName.substring(1) + "()";

            if (typeName.equals("int")) {
                out.println("            dos.writeInt(" + getter + ");");
            } else if (typeName.equals("java.lang.String")) {
                out.println("            dos.writeUTF(" + getter + ");");
            } else if (typeName.equals("java.time.LocalDate")) {
                out.println("            dos.writeUTF(" + getter + ".toString()); // Store as ISO String");
            } else if (typeName.startsWith("java.util.List")) {
                // Simplified List serialization (assuming List<T> where T is simple or a custom class)
                // WARNING: This simplified logic relies on the List being a simple List<T>
                out.println("            dos.writeInt(" + getter + ".size());");
                out.println("            for (Object item : " + getter + ") {");

                // Simplified logic to deduce the item type for serialization.
                // In a real processor, we would need to inspect the generic type argument.
                String genericType = extractGenericType(typeName);
                if (genericType != null) {
                    if (genericType.equals("com.example.PhoneNumber")) { // Assuming custom class path
                        out.println("                com.example.PhoneNumberSerializer.serialize(dos, (com.example.PhoneNumber)item);");
                    } else if (genericType.equals("java.lang.String")) {
                        out.println("                dos.writeUTF((String)item);");
                    } else {
                        out.println("                // WARNING: Type " + genericType + " not fully supported in list. Using standard Object stream.");
                        out.println("                dos.writeUTF(((Object)item).toString());");
                    }
                }
                out.println("            }");

            } else {
                // Assume custom object types are also @ByteSerializable and have a static Serializer method
                out.println("            " + typeName + "Serializer.serialize(dos, " + getter + ");");
            }
        }

        out.println("            dos.flush();");
        out.println("            return bos.toByteArray();");
        out.println("        }");
        out.println("    }");
    }
    
    // Helper method for custom object serialization inside lists
    private void writeCustomObjectSerializeHelper(PrintWriter out, String className, List<VariableElement> fields) {
        out.println();
        out.println("    public static void serialize(DataOutputStream dos, " + className + " obj) throws IOException {");
        for (VariableElement field : fields) {
            String fieldName = field.getSimpleName().toString();
            String typeName = field.asType().toString();
            String getter = "obj.get" + fieldName.substring(0, 1).toUpperCase() + fieldName.substring(1) + "()";

            // Simplified: only support primitives/String/LocalDate/CustomObj. No List in this helper.
            if (typeName.equals("int")) {
                out.println("        dos.writeInt(" + getter + ");");
            } else if (typeName.equals("java.lang.String")) {
                out.println("        dos.writeUTF(" + getter + ");");
            } else if (typeName.equals("java.time.LocalDate")) {
                out.println("        dos.writeUTF(" + getter + ".toString());");
            } else if (!typeName.startsWith("java.util.List")) {
                 out.println("        " + typeName + "Serializer.serialize(dos, " + getter + ");");
            } else {
                 out.println("        // Unsupported field type in nested class helper: " + typeName);
            }
        }
        out.println("    }");
    }


    private void writeDeserializationMethod(PrintWriter out, String className, List<VariableElement> fields) {
        // Generate the custom helper method for custom objects first
        if (!className.equals("Person")) { // Only generate helper for nested classes
             writeCustomObjectDeserializeHelper(out, className, fields);
        }
        
        // Main deserialization method
        out.println();
        out.println("    public static " + className + " deserialize(byte[] bytes) throws IOException {");
        out.println("        try (ByteArrayInputStream bis = new ByteArrayInputStream(bytes);");
        out.println("             DataInputStream dis = new DataInputStream(bis)) {");
        out.println("            return deserialize(dis);"); // Delegate to the stream-based helper
        out.println("        }");
        out.println("    }");

        // Stream-based helper for both main and nested object deserialization
        out.println();
        out.println("    public static " + className + " deserialize(DataInputStream dis) throws IOException {");

        for (VariableElement field : fields) {
            String fieldName = field.getSimpleName().toString();
            String typeName = field.asType().toString();
            String rawType = field.asType().getKind() == TypeKind.DECLARED ? ((TypeElement) processingEnv.getTypeUtils().asElement(field.asType())).getQualifiedName().toString() : typeName;

            out.print("        final " + typeName + " " + fieldName + " = ");

            if (typeName.equals("int")) {
                out.println("dis.readInt();");
            } else if (typeName.equals("java.lang.String")) {
                out.println("dis.readUTF();");
            } else if (typeName.equals("java.time.LocalDate")) {
                out.println("LocalDate.parse(dis.readUTF());");
            } else if (typeName.startsWith("java.util.List")) {
                // Simplified List deserialization
                out.println("new ArrayList<>();");
                out.println("        final int " + fieldName + "Size = dis.readInt();");
                out.println("        for (int i = 0; i < " + fieldName + "Size; i++) {");
                
                String genericType = extractGenericType(typeName);
                if (genericType != null) {
                    if (genericType.equals("com.example.PhoneNumber")) {
                        out.println("            " + fieldName + ".add(" + genericType + "Serializer.deserialize(dis));");
                    } else if (genericType.equals("java.lang.String")) {
                        out.println("            " + fieldName + ".add(dis.readUTF());");
                    } else {
                        out.println("            // WARNING: Type " + genericType + " not fully supported in list.");
                        out.println("            // Fallback for custom type in list: requires a dedicated deserialize(dis) method.");
                        out.println("            // Assuming a simple String for the unhandled case for compilation purposes.");
                        out.println("            " + fieldName + ".add(dis.readUTF());"); 
                    }
                }

                out.println("        }");
                out.print("        // Skipping instantiation here, it's already done: "); // Reset the assignment line
                out.println(); // Add a newline after the loop
            } else {
                // Assume custom object types are also @ByteSerializable
                out.println(typeName + "Serializer.deserialize(dis);");
            }
        }

        // Generate the constructor call to create the final object (Lombok @Value creates an all-args constructor)
        String constructorArgs = fields.stream()
                .map(e -> e.getSimpleName().toString())
                .collect(Collectors.joining(", "));

        out.println("        return new " + className + "(" + constructorArgs + ");");
        out.println("    }");
    }
    
    // Helper method for custom object deserialization (used inside the main Person logic)
    private void writeCustomObjectDeserializeHelper(PrintWriter out, String className, List<VariableElement> fields) {
        out.println();
        out.println("    public static void deserialize(DataInputStream dis, " + className + " obj) throws IOException {");
        // This helper is for *deserializing into* an existing object, which contradicts Lombok @Value's immutable nature.
        // The correct approach is to use the all-args constructor, as done in the main deserialize method.
        // Therefore, we only need the stream-based static factory method `deserialize(DataInputStream dis)`.
    }

    private String extractGenericType(String typeName) {
        int start = typeName.indexOf("<");
        int end = typeName.lastIndexOf(">");
        if (start != -1 && end != -1 && start < end) {
            return typeName.substring(start + 1, end).trim();
        }
        return null;
    }
}