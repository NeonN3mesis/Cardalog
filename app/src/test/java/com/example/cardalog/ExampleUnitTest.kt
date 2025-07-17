package com.example.cardalog

import org.junit.Test

import org.junit.Assert.*

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {
    @Test
    fun addition_isCorrect() {
        assertEquals(4, 2 + 2)
    }

    @Test
    fun businessCardInfoStoresData() {
        val name = "John Doe"
        val title = "CEO"
        val company = "ExampleCorp"
        val phone = "555-555-5555"
        val email = "john@example.com"
        val website = "www.example.com"
        val address = "123 Example St"

        val info = BusinessCardInfo(name, title, company, phone, email, website, address)

        assertEquals(name, info.name)
        assertEquals(title, info.jobTitle)
        assertEquals(company, info.businessName)
        assertEquals(phone, info.phoneNumber)
        assertEquals(email, info.email)
        assertEquals(website, info.website)
        assertEquals(address, info.address)
    }
}