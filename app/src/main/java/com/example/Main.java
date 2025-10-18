// src/main/java/com/example/Person.java
package com.example;

import com.example.annotations.ByteSerializable;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;


public class Main {
    public static void main(String[] args) throws IOException {
        PhoneNumber work = new PhoneNumber("555-1234", 1);
        Address home = new Address("Main St", 101);

        Person original = new Person(
            LocalDate.of(1990, 1, 1), 
            "Alice", 
            2, 
            home, 
            List.of(work, new PhoneNumber("555-9999", 1))
        );

        // --- SERIALIZE ---
        /*byte[] data = PersonSerializer.serialize(original);
        System.out.println("Serialized size: " + data.length + " bytes.");

        // --- DESERIALIZE ---
        Person deserialized = PersonSerializer.deserialize(data);
        
        // --- VERIFY ---
        System.out.println("Original: " + original);
        System.out.println("Deserialized: " + deserialized);
        System.out.println("Match: " + original.equals(deserialized)); // Should be true thanks to Lombok*/
    }
}


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

    public boolean equals(final Object o) {
        if (o == this) return true;
        if (!(o instanceof Person)) return false;
        final Person other = (Person) o;
        final Object this$dateOfBirth = this.getDateOfBirth();
        final Object other$dateOfBirth = other.getDateOfBirth();
        if (this$dateOfBirth == null ? other$dateOfBirth != null : !this$dateOfBirth.equals(other$dateOfBirth)) return false;
        final Object this$name = this.getName();
        final Object other$name = other.getName();
        if (this$name == null ? other$name != null : !this$name.equals(other$name)) return false;
        if (this.getNumberOfArms() != other.getNumberOfArms()) return false;
        final Object this$address = this.getAddress();
        final Object other$address = other.getAddress();
        if (this$address == null ? other$address != null : !this$address.equals(other$address)) return false;
        final Object this$phoneNumbers = this.getPhoneNumbers();
        final Object other$phoneNumbers = other.getPhoneNumbers();
        if (this$phoneNumbers == null ? other$phoneNumbers != null : !this$phoneNumbers.equals(other$phoneNumbers)) return false;
        return true;
    }

    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final Object $dateOfBirth = this.getDateOfBirth();
        result = result * PRIME + ($dateOfBirth == null ? 43 : $dateOfBirth.hashCode());
        final Object $name = this.getName();
        result = result * PRIME + ($name == null ? 43 : $name.hashCode());
        result = result * PRIME + this.getNumberOfArms();
        final Object $address = this.getAddress();
        result = result * PRIME + ($address == null ? 43 : $address.hashCode());
        final Object $phoneNumbers = this.getPhoneNumbers();
        result = result * PRIME + ($phoneNumbers == null ? 43 : $phoneNumbers.hashCode());
        return result;
    }

    public String toString() {return "Person(dateOfBirth=" + this.getDateOfBirth() + ", name=" + this.getName() + ", numberOfArms=" + this.getNumberOfArms() + ", address=" + this.getAddress() + ", phoneNumbers=" + this.getPhoneNumbers() + ")";}
}

@ByteSerializable
final class Address {
    private final String street;
    private final int number;

    public Address(String street, int number) {
        this.street = street;
        this.number = number;
    }

    public String getStreet() {return this.street;}

    public int getNumber() {return this.number;}

    public boolean equals(final Object o) {
        if (o == this) return true;
        if (!(o instanceof Address)) return false;
        final Address other = (Address) o;
        final Object this$street = this.getStreet();
        final Object other$street = other.getStreet();
        if (this$street == null ? other$street != null : !this$street.equals(other$street)) return false;
        if (this.getNumber() != other.getNumber()) return false;
        return true;
    }

    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final Object $street = this.getStreet();
        result = result * PRIME + ($street == null ? 43 : $street.hashCode());
        result = result * PRIME + this.getNumber();
        return result;
    }

    public String toString() {return "Address(street=" + this.getStreet() + ", number=" + this.getNumber() + ")";}
}

@ByteSerializable
final class PhoneNumber {
    private final String number;
    private final int countryCode;

    public PhoneNumber(String number, int countryCode) {
        this.number = number;
        this.countryCode = countryCode;
    }

    public String getNumber() {return this.number;}

    public int getCountryCode() {return this.countryCode;}

    public boolean equals(final Object o) {
        if (o == this) return true;
        if (!(o instanceof PhoneNumber)) return false;
        final PhoneNumber other = (PhoneNumber) o;
        final Object this$number = this.getNumber();
        final Object other$number = other.getNumber();
        if (this$number == null ? other$number != null : !this$number.equals(other$number)) return false;
        if (this.getCountryCode() != other.getCountryCode()) return false;
        return true;
    }

    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final Object $number = this.getNumber();
        result = result * PRIME + ($number == null ? 43 : $number.hashCode());
        result = result * PRIME + this.getCountryCode();
        return result;
    }

    public String toString() {return "PhoneNumber(number=" + this.getNumber() + ", countryCode=" + this.getCountryCode() + ")";}
}