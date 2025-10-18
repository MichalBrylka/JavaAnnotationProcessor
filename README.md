# Purpose
This repository contains code and resources for simple annotation processor that will be able to generate serialization and deserialization logic for simple POJO classes

Given following class annotated with `@ByteSerializable`:
```java
@ByteSerializable
final class Person {
    private final LocalDate dateOfBirth;
    private final String name;
    private final int numberOfArms;
    private final Address address;
    private final List<PhoneNumber> phoneNumbers;

    public Person(LocalDate dateOfBirth, String name, int numberOfArms, Address address, List<PhoneNumber> phoneNumbers) {
        this.dateOfBirth = dateOfBirth;
        this.name = name;
        this.numberOfArms = numberOfArms;
        this.address = address;
        this.phoneNumbers = phoneNumbers;
    }

    public LocalDate getDateOfBirth() {return this.dateOfBirth;}

    public String getName() {return this.name;}

    public int getNumberOfArms() {return this.numberOfArms;}

    public Address getAddress() {return this.address;}

    public List<PhoneNumber> getPhoneNumbers() {return this.phoneNumbers;}    
}

final class Address {
    private final String street;
    private final int number;

    public Address(String street, int number) {
        this.street = street;
        this.number = number;
    }

    public String getStreet() {return this.street;}

    public int getNumber() {return this.number;}
}

final class PhoneNumber {
    private final String number;
    private final int countryCode;

    public PhoneNumber(String number, int countryCode) {
        this.number = number;
        this.countryCode = countryCode;
    }

    public String getNumber() {return this.number;}

    public int getCountryCode() {return this.countryCode;}    
}
```
processor will generate the following serialization code:

```java
package com.example;

import java.io.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public final class PersonSerializer {
    private PersonSerializer() { throw new UnsupportedOperationException(); }

    public static byte[] serialize(Person obj) throws IOException {
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
             DataOutputStream dos = new DataOutputStream(bos)) {
            serialize(dos, obj);
            dos.flush();
            return bos.toByteArray();
        }
    }

    public static void serialize(DataOutputStream dos, Person obj) throws IOException {
        dos.writeUTF(obj.getDateOfBirth().toString());
        dos.writeUTF(obj.getName());
        dos.writeInt(obj.getNumberOfArms());
        serializeAddress(dos, obj.getAddress());
        dos.writeInt(obj.getPhoneNumbers().size());
        for (com.example.PhoneNumber item : obj.getPhoneNumbers()) {
            serializePhoneNumber(dos, item);
        }
    }

    public static Person deserialize(byte[] bytes) throws IOException {
        try (ByteArrayInputStream bis = new ByteArrayInputStream(bytes);
             DataInputStream dis = new DataInputStream(bis)) {
            return deserialize(dis);
        }
    }

    public static Person deserialize(DataInputStream dis) throws IOException {
        final java.time.LocalDate dateOfBirth = LocalDate.parse(dis.readUTF());
        final java.lang.String name = dis.readUTF();
        final int numberOfArms = dis.readInt();
        final com.example.Address address = deserializeAddress(dis);
        final java.util.List<com.example.PhoneNumber> phoneNumbers = new ArrayList<>();
        final int phoneNumbersSize = dis.readInt();
        for (int i = 0; i < phoneNumbersSize; i++) {
            phoneNumbers.add(deserializePhoneNumber(dis));
        }
        return new Person(dateOfBirth, name, numberOfArms, address, phoneNumbers);
    }

    private static void serializeAddress(DataOutputStream dos, Address obj) throws IOException {
        dos.writeUTF(obj.getStreet());
        dos.writeInt(obj.getNumber());
    }

    private static Address deserializeAddress(DataInputStream dis) throws IOException {
        final java.lang.String street = dis.readUTF();
        final int number = dis.readInt();
        return new Address(street, number);
    }

    private static void serializePhoneNumber(DataOutputStream dos, PhoneNumber obj) throws IOException {
        dos.writeUTF(obj.getNumber());
        dos.writeInt(obj.getCountryCode());
    }

    private static PhoneNumber deserializePhoneNumber(DataInputStream dis) throws IOException {
        final java.lang.String number = dis.readUTF();
        final int countryCode = dis.readInt();
        return new PhoneNumber(number, countryCode);
    }
}

```