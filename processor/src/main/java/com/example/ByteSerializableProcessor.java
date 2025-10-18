package com.example;

import com.example.annotations.ByteSerializable;
import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.*;
import javax.lang.model.type.DeclaredType;
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
@SupportedSourceVersion(SourceVersion.RELEASE_21)
public class ByteSerializableProcessor extends AbstractProcessor {

    private TypeElement addressElement;
    private TypeElement phoneNumberElement;

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE, "ByteSerializableProcessor: processing annotations...");
        // Step 1: Find all required type elements *before* processing
        addressElement = processingEnv.getElementUtils().getTypeElement("com.example.Address");
        phoneNumberElement = processingEnv.getElementUtils().getTypeElement("com.example.PhoneNumber");

        for (Element element : roundEnv.getElementsAnnotatedWith(ByteSerializable.class)) {
            if (element.getKind() == ElementKind.CLASS) {
                TypeElement typeElement = (TypeElement) element;
                try {
                    // Only generate for the annotated class (Person)
                    if (typeElement.getSimpleName().contentEquals("Person")) {
                        generateFlatSerializer(typeElement);
                    }
                } catch (IOException e) {
                    processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "Failed to generate serializer for " + typeElement.getQualifiedName() + ": " + e.getMessage());
                }
            }
        }
        return true;
    }

    private void generateFlatSerializer(TypeElement typeElement) throws IOException {
        String packageName = processingEnv.getElementUtils().getPackageOf(typeElement).getQualifiedName().toString();
        String className = typeElement.getSimpleName().toString();
        String serializerClassName = className + "Serializer";
        String fullSerializerClassName = packageName.isEmpty() ? serializerClassName : packageName + "." + serializerClassName;

        JavaFileObject builderFile = processingEnv.getFiler().createSourceFile(fullSerializerClassName);

        try (PrintWriter out = new PrintWriter(builderFile.openWriter())) {
            List<VariableElement> fields = getRelevantFields(typeElement);

            writeClassHeader(out, packageName, className, serializerClassName);

            // 1. Serialization Method (Main Entry)
            writeMainSerializationMethod(out, className, fields);

            // 2. Deserialization Method (Main Entry)
            writeMainDeserializationMethod(out, className);

            // 3. Helper Methods for Nested Types (Address and PhoneNumber)
            writeNestedSerializationHelper(out, addressElement);
            writeNestedDeserializationHelper(out, addressElement);
            writeNestedSerializationHelper(out, phoneNumberElement);
            writeNestedDeserializationHelper(out, phoneNumberElement);

            writeClassFooter(out);
        }
    }

    // --- Utility Methods ---

    private List<VariableElement> getRelevantFields(TypeElement typeElement) {
        return typeElement.getEnclosedElements().stream()
                .filter(e -> e.getKind() == ElementKind.FIELD)
                .map(e -> (VariableElement) e)
                .collect(Collectors.toList());
    }

    private String extractGenericType(TypeMirror type) {
        if (type.getKind() == TypeKind.DECLARED) {
            DeclaredType declaredType = (DeclaredType) type;
            List<? extends TypeMirror> typeArguments = declaredType.getTypeArguments();
            if (!typeArguments.isEmpty()) {
                return typeArguments.getFirst().toString();
            }
        }
        return null;
    }

    // --- Code Generation Writers (Refactored) ---

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
        out.println("public final class " + serializerClassName + " {");
        out.println("    private " + serializerClassName + "() { throw new UnsupportedOperationException(); }"); // Utility class
    }

    private void writeClassFooter(PrintWriter out) {
        out.println("}");
    }

    // --- Main Serialization Logic (Person) ---

    private void writeMainSerializationMethod(PrintWriter out, String className, List<VariableElement> fields) {
        out.println();
        out.println("    public static byte[] serialize(" + className + " obj) throws IOException {");
        out.println("        try (ByteArrayOutputStream bos = new ByteArrayOutputStream();");
        out.println("             DataOutputStream dos = new DataOutputStream(bos)) {");

        // Delegate to the stream helper
        out.println("            serialize(dos, obj);");

        out.println("            dos.flush();");
        out.println("            return bos.toByteArray();");
        out.println("        }");
        out.println("    }");

        out.println();
        out.println("    public static void serialize(DataOutputStream dos, " + className + " obj) throws IOException {");
        for (VariableElement field : fields) {
            writeSerializationField(out, field, "obj");
        }
        out.println("    }");
    }

    // --- Main Deserialization Logic (Person) ---

    private void writeMainDeserializationMethod(PrintWriter out, String className) {
        out.println();
        out.println("    public static " + className + " deserialize(byte[] bytes) throws IOException {");
        out.println("        try (ByteArrayInputStream bis = new ByteArrayInputStream(bytes);");
        out.println("             DataInputStream dis = new DataInputStream(bis)) {");
        out.println("            return deserialize(dis);");
        out.println("        }");
        out.println("    }");

        // Delegate stream helper (called by main and potentially others if Person was nested)
        out.println();
        out.println("    public static " + className + " deserialize(DataInputStream dis) throws IOException {");

        List<VariableElement> fields = getRelevantFields(processingEnv.getElementUtils().getTypeElement("com.example.Person"));

        for (VariableElement field : fields) {
            writeDeserializationField(out, field);
        }

        // Constructor call
        String constructorArgs = fields.stream()
                .map(e -> e.getSimpleName().toString())
                .collect(Collectors.joining(", "));

        out.println("        return new " + className + "(" + constructorArgs + ");");
        out.println("    }");
    }

    // --- Nested Type Helper Generators ---

    private void writeNestedSerializationHelper(PrintWriter out, TypeElement typeElement) {
        String className = typeElement.getSimpleName().toString();
        List<VariableElement> fields = getRelevantFields(typeElement);

        out.println();
        out.println("    private static void serialize" + className + "(DataOutputStream dos, " + className + " obj) throws IOException {");
        for (VariableElement field : fields) {
            writeSerializationField(out, field, "obj");
        }
        out.println("    }");
    }

    private void writeNestedDeserializationHelper(PrintWriter out, TypeElement typeElement) {
        String className = typeElement.getSimpleName().toString();
        List<VariableElement> fields = getRelevantFields(typeElement);

        out.println();
        out.println("    private static " + className + " deserialize" + className + "(DataInputStream dis) throws IOException {");

        for (VariableElement field : fields) {
            writeDeserializationField(out, field);
        }

        // Constructor call
        String constructorArgs = fields.stream()
                .map(e -> e.getSimpleName().toString())
                .collect(Collectors.joining(", "));

        out.println("        return new " + className + "(" + constructorArgs + ");");
        out.println("    }");
    }

    // --- Field Serialization Logic (Centralized) ---

    private void writeSerializationField(PrintWriter out, VariableElement field, String objectName) {
        String fieldName = field.getSimpleName().toString();
        String typeName = field.asType().toString();
        String getter = objectName + ".get" + fieldName.substring(0, 1).toUpperCase() + fieldName.substring(1) + "()";

        if (typeName.equals("int")) {
            out.println("        dos.writeInt(" + getter + ");");
        } else if (typeName.equals("java.lang.String")) {
            out.println("        dos.writeUTF(" + getter + ");");
        } else if (typeName.equals("java.time.LocalDate")) {
            out.println("        dos.writeUTF(" + getter + ".toString());");
        } else if (typeName.equals("com.example.Address")) {
            out.println("        serializeAddress(dos, " + getter + ");");
        } else if (typeName.startsWith("java.util.List")) {
            // Simplified List serialization
            out.println("        dos.writeInt(" + getter + ".size());");
            out.println("        for (com.example.PhoneNumber item : " + getter + ") {");
            out.println("            serializePhoneNumber(dos, item);");
            out.println("        }");
        } else {
            // Fallback for unsupported types, should ideally throw an error
            processingEnv.getMessager().printMessage(Diagnostic.Kind.WARNING, "Unsupported field type encountered: " + typeName + " in " + objectName);
        }
    }

    // --- Field Deserialization Logic (Centralized) ---

    private void writeDeserializationField(PrintWriter out, VariableElement field) {
        String fieldName = field.getSimpleName().toString();
        String typeName = field.asType().toString();

        out.print("        final " + typeName + " " + fieldName + " = ");

        if (typeName.equals("int")) {
            out.println("dis.readInt();");
        } else if (typeName.equals("java.lang.String")) {
            out.println("dis.readUTF();");
        } else if (typeName.equals("java.time.LocalDate")) {
            out.println("LocalDate.parse(dis.readUTF());");
        } else if (typeName.equals("com.example.Address")) {
            out.println("deserializeAddress(dis);");
        } else if (typeName.startsWith("java.util.List")) {
            // Simplified List deserialization (assumes List<PhoneNumber>)
            out.println("new ArrayList<>();");
            out.println("        final int " + fieldName + "Size = dis.readInt();");
            out.println("        for (int i = 0; i < " + fieldName + "Size; i++) {");
            out.println("            " + fieldName + ".add(deserializePhoneNumber(dis));");
            out.println("        }");
        } else {
            // Fallback for unsupported types, ideally throw an error
            out.println("null; // Unsupported field type: " + typeName);
        }
    }
}