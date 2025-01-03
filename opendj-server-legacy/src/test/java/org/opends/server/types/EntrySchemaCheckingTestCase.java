/*
 * The contents of this file are subject to the terms of the Common Development and
 * Distribution License (the License). You may not use this file except in compliance with the
 * License.
 *
 * You can obtain a copy of the License at legal/CDDLv1.0.txt. See the License for the
 * specific language governing permission and limitations under the License.
 *
 * When distributing Covered Software, include this CDDL Header Notice in each file and include
 * the License file at legal/CDDLv1.0.txt. If applicable, add the following below the CDDL
 * Header, with the fields enclosed by brackets [] replaced by your own identifying
 * information: "Portions Copyright [year] [name of copyright owner]".
 *
 * Copyright 2008 Sun Microsystems, Inc.
 * Portions Copyright 2014-2016 ForgeRock AS.
 * Portions Copyright 2024 3A Systems, LLC
 */
package org.opends.server.types;

import static org.opends.server.TestCaseUtils.getServer;
import static org.opends.server.types.NullOutputStream.nullPrintStream;
import static org.testng.Assert.*;

import java.util.LinkedList;

import org.forgerock.i18n.LocalizableMessageBuilder;
import org.forgerock.opendj.config.client.ManagementContext;
import org.forgerock.opendj.ldap.ByteString;
import org.forgerock.opendj.ldap.schema.AttributeType;
import org.forgerock.opendj.ldap.schema.CoreSchema;
import org.forgerock.opendj.server.config.client.GlobalCfgClient;
import org.forgerock.opendj.server.config.meta.GlobalCfgDefn;
import org.opends.server.TestCaseUtils;
import com.forgerock.opendj.ldap.tools.LDAPModify;
import org.testng.annotations.Test;

/**
 * This class provides a set of test cases that cover the schema validation
 * processing that should be performed on entries during add, modify, and
 * modify DN operations.
 */
public class EntrySchemaCheckingTestCase
       extends TypesTestCase
{
  /**
   * Ensures that the provided entry fails schema checking validation with
   * strict compliance enabled, but will pass in a more relaxed configuration.
   *
   * @param  e  The entry to be tested.
   */
  private void failOnlyForStrictEvaluation(Entry e) throws Exception
  {
    try
    {
      LocalizableMessageBuilder invalidReason = new LocalizableMessageBuilder();
      setSingleStructuralObjectClassPolicy(GlobalCfgDefn.SingleStructuralObjectclassBehavior.REJECT);
      assertFalse(e.conformsToSchema(null, false, true, true, invalidReason),
                  "Entry validation succeeded with REJECT policy");

      setSingleStructuralObjectClassPolicy(GlobalCfgDefn.SingleStructuralObjectclassBehavior.WARN);
      assertTrue(e.conformsToSchema(null, false, true, true, invalidReason),
                 "Entry validation failed with WARN policy:  " + invalidReason);

      setSingleStructuralObjectClassPolicy(GlobalCfgDefn.SingleStructuralObjectclassBehavior.ACCEPT);
      assertTrue(e.conformsToSchema(null, false, true, true, invalidReason),
                 "Entry validation failed with ACCEPT policy:  " + invalidReason);
    }
    finally
    {
      setSingleStructuralObjectClassPolicy(GlobalCfgDefn.SingleStructuralObjectclassBehavior.REJECT);
    }
  }

  private void setSingleStructuralObjectClassPolicy(GlobalCfgDefn.SingleStructuralObjectclassBehavior policy)
      throws Exception
  {
    try (ManagementContext conf = getServer().getConfiguration())
    {
      GlobalCfgClient globalCfg = conf.getRootConfiguration().getGlobalConfiguration();
      globalCfg.setSingleStructuralObjectclassBehavior(policy);
      globalCfg.commit();
    }
  }

    /**
     * Ensures that the provided entry fails schema checking validation irrespective
     * of relaxed compliance configured. Added due to unique niche case described
     * in OPENDJ-1797
     *
     * @param  e  The entry to be tested.
     */
    private void failAlwaysStrictEvaluation(Entry e) throws Exception
    {
        try
        {
            LocalizableMessageBuilder invalidReason = new LocalizableMessageBuilder();
            setSingleStructuralObjectClassPolicy(GlobalCfgDefn.SingleStructuralObjectclassBehavior.REJECT);
            assertFalse(e.conformsToSchema(null, false, true, true, invalidReason),
                    "Entry validation succeeded with REJECT policy");

            setSingleStructuralObjectClassPolicy(GlobalCfgDefn.SingleStructuralObjectclassBehavior.WARN);
            assertFalse(e.conformsToSchema(null, false, true, true, invalidReason),
                    "Entry validation failed with WARN policy:  " + invalidReason);

            setSingleStructuralObjectClassPolicy(GlobalCfgDefn.SingleStructuralObjectclassBehavior.ACCEPT);
            assertFalse(e.conformsToSchema(null, false, true, true, invalidReason),
                    "Entry validation failed with ACCEPT policy:  " + invalidReason);
        }
        finally
        {
          setSingleStructuralObjectClassPolicy(GlobalCfgDefn.SingleStructuralObjectclassBehavior.REJECT);
        }
    }



  /**
   * Tests schema checking for an entry with a valid single structural
   * objectclass.
   *
   * @throws  Exception  If an unexpected problem occurs.
   */
  @Test
  public void testValidSingleStructuralClass()
         throws Exception
  {
    Entry e = TestCaseUtils.makeEntry(
         "dn: dc=example,dc=com",
         "objectClass: top",
         "objectClass: domain",
         "dc: example");

    LocalizableMessageBuilder invalidReason = new LocalizableMessageBuilder();
    assertTrue(e.conformsToSchema(null, true, true, true, invalidReason),
               invalidReason.toString());
  }



  /**
   * Tests schema checking for an entry (not covered by a DIT content rule)
   * with a valid single structural objectclass as well as an auxiliary
   * objectclass.
   *
   * @throws  Exception  If an unexpected problem occurs.
   */
  @Test
  public void testValidSingleStructuralClassAndAuxiliaryClass()
         throws Exception
  {
    Entry e = TestCaseUtils.makeEntry(
         "dn: dc=example,dc=com",
         "objectClass: top",
         "objectClass: organization",
         "objectClass: dcObject",
         "dc: example",
         "o: Example Org");

    LocalizableMessageBuilder invalidReason = new LocalizableMessageBuilder();
    assertTrue(e.conformsToSchema(null, true, true, true, invalidReason),
               invalidReason.toString());
  }



  /**
   * Tests schema checking for an entry that does not contain a structural
   * objectclass.
   *
   * @throws  Exception  If an unexpected problem occurs.
   */
  @Test
  public void testNoStructuralClass()
         throws Exception
  {
    Entry e = TestCaseUtils.makeEntry(
         "dn: dc=example,dc=com",
         "objectClass: top",
         "objectClass: dcObject",
         "dc: example");

    failOnlyForStrictEvaluation(e);
  }



  /**
   * Tests schema checking for an entry that contains multiple structural
   * objectclasses.
   *
   * @throws  Exception  If an unexpected problem occurs.
   */
  @Test
  public void testMultipleStructuralClasses()
         throws Exception
  {
    TestCaseUtils.initializeTestBackend(true);

    Entry e = TestCaseUtils.makeEntry(
         "dn: uid=test.user,o=test",
         "objectClass: top",
         "objectClass: person",
         "objectClass: organizationalPerson",
         "objectClass: inetOrgPerson",
         "objectClass: account",
         "uid: test.user",
         "givenName: Test",
         "sn: User",
         "cn: Test User");

    failOnlyForStrictEvaluation(e);
  }



  /**
   * Tests schema checking for an entry that contains an undefined objectclass
   * with no other structural class.
   *
   * @throws  Exception  If an unexpected problem occurs.
   */
  @Test
  public void testUndefinedStructuralObjectClass()
         throws Exception
  {
    TestCaseUtils.initializeTestBackend(true);

    Entry e = TestCaseUtils.makeEntry(
         "dn: cn=test,o=test",
         "objectClass: top",
         "objectClass: xxxundefinedstructuralxxx",
         "cn: test");

    failAlwaysStrictEvaluation(e);
  }



  /**
   * Tests schema checking for an entry that contains an undefined objectclass
   * as well as a valid structural class.
   *
   * @throws  Exception  If an unexpected problem occurs.
   */
  @Test
  public void testUndefinedAuxiliaryObjectClass()
         throws Exception
  {
    TestCaseUtils.initializeTestBackend(true);

    Entry e = TestCaseUtils.makeEntry(
         "dn: cn=test,o=test",
         "objectClass: top",
         "objectClass: device",
         "objectClass: xxxundefinedauxiliaryxxx",
         "cn: test");

    failAlwaysStrictEvaluation(e);
  }



  /**
   * Tests schema checking for an entry that is missing an attribute required
   * by its structural object class.
   *
   * @throws  Exception  If an unexpected problem occurs.
   */
  @Test
  public void testMissingAttributeRequiredByStructuralClass()
         throws Exception
  {
    TestCaseUtils.initializeTestBackend(true);

    String path = TestCaseUtils.createTempFile(
         "dn: cn=schema",
         "changetype: modify",
         "add: objectClasses",
         "objectClasses:  ( testmissingatrequiredbystructuraloc-oid " +
              "NAME 'testMissingATRequiredByStructuralOC' SUP top STRUCTURAL " +
              "MUST ( cn $ description ) " +
              "X-ORIGIN 'EntrySchemaCheckingTestCase')");

    String[] args =
    {
      "-h", "127.0.0.1",
      "-p", String.valueOf(TestCaseUtils.getServerLdapPort()),
      "-D", "cn=Directory Manager",
      "-w", "password",
      "-f", path
    };

    assertEquals(LDAPModify.run(nullPrintStream(), System.err, args), 0);

    Entry e = TestCaseUtils.makeEntry(
         "dn: cn=test,o=test",
         "objectClass: top",
         "objectClass: testMissingATRequiredByStructuralOC",
         "cn: test");

    assertFalse(e.conformsToSchema(null, false, true, true,
                                   new LocalizableMessageBuilder()));
  }



  /**
   * Tests schema checking for an entry that is missing an attribute required
   * by an auxiliary object class.
   *
   * @throws  Exception  If an unexpected problem occurs.
   */
  @Test
  public void testMissingAttributeRequiredByAuxiliaryClass()
         throws Exception
  {
    TestCaseUtils.initializeTestBackend(true);

    String path = TestCaseUtils.createTempFile(
         "dn: cn=schema",
         "changetype: modify",
         "add: objectClasses",
         "objectClasses:  ( testmissingatrequiredbyauxiliaryoc-oid " +
              "NAME 'testMissingATRequiredByAuxiliaryOC' SUP top AUXILIARY " +
              "MUST ( cn $ description ) " +
              "X-ORIGIN 'EntrySchemaCheckingTestCase')");

    String[] args =
    {
      "-h", "127.0.0.1",
      "-p", String.valueOf(TestCaseUtils.getServerLdapPort()),
      "-D", "cn=Directory Manager",
      "-w", "password",
      "-f", path
    };

    assertEquals(LDAPModify.run(nullPrintStream(), System.err, args), 0);

    Entry e = TestCaseUtils.makeEntry(
         "dn: cn=test,o=test",
         "objectClass: top",
         "objectClass: device",
         "objectClass: testMissingATRequiredByAuxiliaryOC",
         "cn: test");

    assertFalse(e.conformsToSchema(null, false, true, true,
                                   new LocalizableMessageBuilder()));
  }



  /**
   * Tests schema checking for an entry that includes an attribute type that
   * is not allowed by any of its object classes.
   *
   * @throws  Exception  If an unexpected problem occurs.
   */
  @Test
  public void testDisallowedAttributeType()
         throws Exception
  {
    TestCaseUtils.initializeTestBackend(true);

    String path = TestCaseUtils.createTempFile(
         "dn: cn=schema",
         "changetype: modify",
         "add: objectClasses",
         "objectClasses:  ( testdisallowedattributetypeoc-oid " +
              "NAME 'testDisallowedAttributeTypeOC' SUP top STRUCTURAL " +
              "MUST cn X-ORIGIN 'EntrySchemaCheckingTestCase')");

    String[] args =
    {
      "-h", "127.0.0.1",
      "-p", String.valueOf(TestCaseUtils.getServerLdapPort()),
      "-D", "cn=Directory Manager",
      "-w", "password",
      "-f", path
    };

    assertEquals(LDAPModify.run(nullPrintStream(), System.err, args), 0);

    Entry e = TestCaseUtils.makeEntry(
         "dn: cn=test,o=test",
         "objectClass: top",
         "objectClass: testDisallowedAttributeTypeOC",
         "cn: test",
         "description: foo");

    assertFalse(e.conformsToSchema(null, false, true, true,
                                   new LocalizableMessageBuilder()));
  }



  /**
   * Tests schema checking for an entry that includes multiple values for a
   * multivalued attribute.
   *
   * @throws  Exception  If an unexpected problem occurs.
   */
  @Test
  public void testMultipleValuesForMultiValuedAttribute()
         throws Exception
  {
    Entry e = TestCaseUtils.makeEntry(
         "dn: o=test",
         "objectClass: top",
         "objectClass: organization",
         "o: test",
         "o: foo");

    LocalizableMessageBuilder invalidReason = new LocalizableMessageBuilder();
    assertTrue(e.conformsToSchema(null, true, true, true, invalidReason),
               invalidReason.toString());
  }



  /**
   * Tests schema checking for an entry that includes multiple values for a
   * single-valued attribute.
   *
   * @throws  Exception  If an unexpected problem occurs.
   */
  @Test
  public void testMultipleValuesForSingleValuedAttribute()
         throws Exception
  {
    // The LDIF reader won't let us do this directly, so we have to hack around
    // it.
    Entry e = TestCaseUtils.makeEntry(
         "dn: dc=example,dc=com",
         "objectClass: top",
         "objectClass: domain",
         "dc: example");

    e.addAttribute(Attributes.create("dc", "foo"), new LinkedList<ByteString>());

    assertFalse(e.conformsToSchema(null, false, true, true, new LocalizableMessageBuilder()));
  }



  /**
   * Tests schema checking for an entry that includes multiple values for a
   * single-valued operational attribute.
   *
   * @throws  Exception  If an unexpected problem occurs.
   */
  @Test
  public void testMultipleValuesForSingleValuedOperationalAttribute()
         throws Exception
  {
    // The LDIF reader won't let us do this directly, so we have to hack around it.
    Entry e = TestCaseUtils.makeEntry(
         "dn: dc=example,dc=com",
         "objectClass: top",
         "objectClass: domain",
         "dc: example");

    AttributeType creatorsNameType = CoreSchema.getCreatorsNameAttributeType();
    assertTrue(creatorsNameType.isOperational());

    AttributeBuilder builder = new AttributeBuilder(creatorsNameType);
    builder.add("cn=Directory Manager");
    builder.add("cn=Another User");
    e.addAttribute(builder.toAttribute(), new LinkedList<ByteString>());

    assertFalse(e.conformsToSchema(null, false, true, true, new LocalizableMessageBuilder()));
  }



  /**
   * Tests schema checking for an entry that contains structural and auxiliary
   * objectclasses where the auxiliary class is allowed by a DIT content rule.
   *
   * @throws  Exception  If an unexpected problem occurs.
   */
  @Test
  public void testAuxiliaryClassAllowedByDCR()
         throws Exception
  {
    TestCaseUtils.initializeTestBackend(true);

    String path = TestCaseUtils.createTempFile(
         "dn: cn=schema",
         "changetype: modify",
         "add: objectClasses",
         "objectClasses:  ( testauxiliaryclassallowedbydcroc-oid " +
              "NAME 'testAuxiliaryClassAllowedByDCROC' SUP top STRUCTURAL " +
              "MUST cn X-ORIGIN 'EntrySchemaCheckingTestCase')",
         "objectClasses:  ( testauxiliaryclassallowedbydcrocaux-oid " +
              "NAME 'testAuxiliaryClassAllowedByDCROCAux' SUP top AUXILIARY " +
              "MAY description X-ORIGIN 'EntrySchemaCheckingTestCase')",
         "-",
         "add: ditContentRules",
         "ditContentRules: ( testauxiliaryclassallowedbydcroc-oid " +
              "NAME 'testAuxiliaryClassAllowedByDCR' " +
              "AUX testAuxiliaryClassAllowedByDCROCAux " +
              "X-ORIGIN 'EntrySchemaCheckingTestCase' )");

    String[] args =
    {
      "-h", "127.0.0.1",
      "-p", String.valueOf(TestCaseUtils.getServerLdapPort()),
      "-D", "cn=Directory Manager",
      "-w", "password",
      "-f", path
    };

    assertEquals(LDAPModify.run(nullPrintStream(), System.err, args), 0);

    Entry e = TestCaseUtils.makeEntry(
         "dn: cn=test,o=test",
         "objectClass: top",
         "objectClass: testAuxiliaryClassAllowedByDCROC",
         "objectClass: testAuxiliaryClassAllowedByDCROCAux",
         "cn: test");

    LocalizableMessageBuilder invalidReason = new LocalizableMessageBuilder();
    assertTrue(e.conformsToSchema(null, false, true, true, invalidReason),
               invalidReason.toString());
  }



  /**
   * Tests schema checking for an entry that contains structural and auxiliary
   * objectclasses where the auxiliary class is not allowed by the associated
   * DIT content rule.
   *
   * @throws  Exception  If an unexpected problem occurs.
   */
  @Test
  public void testAuxiliaryClassNotAllowedByDCR()
         throws Exception
  {
    TestCaseUtils.initializeTestBackend(true);

    String path = TestCaseUtils.createTempFile(
         "dn: cn=schema",
         "changetype: modify",
         "add: objectClasses",
         "objectClasses:  ( testauxiliaryclassnotallowedbydcroc-oid " +
              "NAME 'testAuxiliaryClassNotAllowedByDCROC' SUP top STRUCTURAL " +
              "MUST cn X-ORIGIN 'EntrySchemaCheckingTestCase')",
         "objectClasses:  ( testauxiliaryclassnotallowedbydcrocaux-oid " +
              "NAME 'testAuxiliaryClassNotAllowedByDCROCAux' SUP top " +
              "AUXILIARY MAY description " +
              "X-ORIGIN 'EntrySchemaCheckingTestCase')",
         "-",
         "add: ditContentRules",
         "ditContentRules: ( testauxiliaryclassnotallowedbydcroc-oid " +
              "NAME 'testAuxiliaryClassNotAllowedByDCR' " +
              "X-ORIGIN 'EntrySchemaCheckingTestCase' )");

    String[] args =
    {
      "-h", "127.0.0.1",
      "-p", String.valueOf(TestCaseUtils.getServerLdapPort()),
      "-D", "cn=Directory Manager",
      "-w", "password",
      "-f", path
    };

    assertEquals(LDAPModify.run(nullPrintStream(), System.err, args), 0);

    Entry e = TestCaseUtils.makeEntry(
         "dn: cn=test,o=test",
         "objectClass: top",
         "objectClass: testAuxiliaryClassNotAllowedByDCROC",
         "objectClass: testAuxiliaryClassNotAllowedByDCROCAux",
         "cn: test");

    failOnlyForStrictEvaluation(e);
  }



  /**
   * Tests schema checking for an entry covered by a DIT content rule to
   * ensure that attributes required by the DIT content rule are allowed even
   * if not directly allowed by any of the entry's objectclasses.
   *
   * @throws  Exception  If an unexpected problem occurs.
   */
  @Test
  public void testAllowAttributeRequiredByDCR()
         throws Exception
  {
    TestCaseUtils.initializeTestBackend(true);

    String path = TestCaseUtils.createTempFile(
         "dn: cn=schema",
         "changetype: modify",
         "add: objectClasses",
         "objectClasses:  ( testallowatrequiredbydcroc-oid " +
              "NAME 'testAllowATRequiredByDCROC' SUP top STRUCTURAL " +
              "MUST cn X-ORIGIN 'EntrySchemaCheckingTestCase')",
         "-",
         "add: ditContentRules",
         "ditContentRules: ( testallowatrequiredbydcroc-oid " +
              "NAME 'testAllowATRequiredByDCR' MUST description " +
              "X-ORIGIN 'EntrySchemaCheckingTestCase' )");

    String[] args =
    {
      "-h", "127.0.0.1",
      "-p", String.valueOf(TestCaseUtils.getServerLdapPort()),
      "-D", "cn=Directory Manager",
      "-w", "password",
      "-f", path
    };

    assertEquals(LDAPModify.run(nullPrintStream(), System.err, args), 0);

    Entry e = TestCaseUtils.makeEntry(
         "dn: cn=test,o=test",
         "objectClass: top",
         "objectClass: testAllowATRequiredByDCROC",
         "cn: test",
         "description: foo");

    LocalizableMessageBuilder invalidReason = new LocalizableMessageBuilder();
    assertTrue(e.conformsToSchema(null, false, true, true, invalidReason),
               invalidReason.toString());
  }



  /**
   * Tests schema checking for an entry covered by a DIT content rule to
   * ensure that attributes required by the DIT content rule are required even
   * if not directly allowed by any of the entry's objectclasses.
   *
   * @throws  Exception  If an unexpected problem occurs.
   */
  @Test
  public void testRequireAttributeRequiredByDCR()
         throws Exception
  {
    TestCaseUtils.initializeTestBackend(true);

    String path = TestCaseUtils.createTempFile(
         "dn: cn=schema",
         "changetype: modify",
         "add: objectClasses",
         "objectClasses:  ( testrequireatrequiredbydcroc-oid " +
              "NAME 'testRequireATRequiredByDCROC' SUP top STRUCTURAL " +
              "MUST cn X-ORIGIN 'EntrySchemaCheckingTestCase')",
         "-",
         "add: ditContentRules",
         "ditContentRules: ( testrequireatrequiredbydcroc-oid " +
              "NAME 'testRequireATRequiredByDCR' MUST description " +
              "X-ORIGIN 'EntrySchemaCheckingTestCase' )");

    String[] args =
    {
      "-h", "127.0.0.1",
      "-p", String.valueOf(TestCaseUtils.getServerLdapPort()),
      "-D", "cn=Directory Manager",
      "-w", "password",
      "-f", path
    };

    assertEquals(LDAPModify.run(nullPrintStream(), System.err, args), 0);

    Entry e = TestCaseUtils.makeEntry(
         "dn: cn=test,o=test",
         "objectClass: top",
         "objectClass: testRequireATRequiredByDCROC",
         "cn: test");

    failOnlyForStrictEvaluation(e);
  }



  /**
   * Tests schema checking for an entry for which there is a DIT content rule
   * covering the structural objectclass but that DIT content rule is marked
   * OBSOLETE.  In this case, any attribute types required by the DIT content
   * rule should not be required for the entry.
   *
   * @throws  Exception  If an unexpected problem occurs.
   */
  @Test
  public void testDontRequireAttributeRequiredByObsoleteDCR()
         throws Exception
  {
    TestCaseUtils.initializeTestBackend(true);

    String path = TestCaseUtils.createTempFile(
         "dn: cn=schema",
         "changetype: modify",
         "add: objectClasses",
         "objectClasses:  ( testdontrequireatrequiredbyobsoletedcroc-oid " +
              "NAME 'testDontRequireATRequiredByObsoleteDCROC' SUP top " +
              "STRUCTURAL MUST cn X-ORIGIN 'EntrySchemaCheckingTestCase')",
         "-",
         "add: ditContentRules",
         "ditContentRules: ( testdontrequireatrequiredbyobsoletedcroc-oid " +
              "NAME 'testDontRequireATRequiredByObsoleteDCR' OBSOLETE " +
              "MUST description X-ORIGIN 'EntrySchemaCheckingTestCase' )");

    String[] args =
    {
      "-h", "127.0.0.1",
      "-p", String.valueOf(TestCaseUtils.getServerLdapPort()),
      "-D", "cn=Directory Manager",
      "-w", "password",
      "-f", path
    };

    assertEquals(LDAPModify.run(nullPrintStream(), System.err, args), 0);

    Entry e = TestCaseUtils.makeEntry(
         "dn: cn=test,o=test",
         "objectClass: top",
         "objectClass: testDontRequireATRequiredByObsoleteDCROC",
         "cn: test");

    LocalizableMessageBuilder invalidReason = new LocalizableMessageBuilder();
    assertTrue(e.conformsToSchema(null, false, true, true, invalidReason),
               invalidReason.toString());
  }



  /**
   * Tests schema checking for an entry covered by a DIT content rule to
   * ensure that attributes allowed by the DIT content rule are allowed even
   * if not directly allowed by any of the entry's objectclasses.
   *
   * @throws  Exception  If an unexpected problem occurs.
   */
  @Test
  public void testAllowAttributeAllowedByDCR()
         throws Exception
  {
    TestCaseUtils.initializeTestBackend(true);

    String path = TestCaseUtils.createTempFile(
         "dn: cn=schema",
         "changetype: modify",
         "add: objectClasses",
         "objectClasses:  ( testallowatallowedbydcroc-oid " +
              "NAME 'testAllowATAllowedByDCROC' SUP top STRUCTURAL " +
              "MUST cn X-ORIGIN 'EntrySchemaCheckingTestCase')",
         "-",
         "add: ditContentRules",
         "ditContentRules: ( testallowatallowedbydcroc-oid " +
              "NAME 'testAllowATAllowedByDCR' MAY description " +
              "X-ORIGIN 'EntrySchemaCheckingTestCase' )");

    String[] args =
    {
      "-h", "127.0.0.1",
      "-p", String.valueOf(TestCaseUtils.getServerLdapPort()),
      "-D", "cn=Directory Manager",
      "-w", "password",
      "-f", path
    };

    assertEquals(LDAPModify.run(nullPrintStream(), System.err, args), 0);

    Entry e = TestCaseUtils.makeEntry(
         "dn: cn=test,o=test",
         "objectClass: top",
         "objectClass: testAllowATAllowedByDCROC",
         "cn: test",
         "description: foo");

    LocalizableMessageBuilder invalidReason = new LocalizableMessageBuilder();
    assertTrue(e.conformsToSchema(null, false, true, true, invalidReason),
               invalidReason.toString());
  }



  /**
   * Tests schema checking for an entry covered by a DIT content rule to
   * ensure that attributes allowed by the DIT content rule are allowed but
   * not required if they are not required by any of the associated object
   * classes.
   *
   * @throws  Exception  If an unexpected problem occurs.
   */
  @Test
  public void testDontRequireAttributeAllowedByDCR()
         throws Exception
  {
    TestCaseUtils.initializeTestBackend(true);

    String path = TestCaseUtils.createTempFile(
         "dn: cn=schema",
         "changetype: modify",
         "add: objectClasses",
         "objectClasses:  ( testdontrequireatallowedbydcroc-oid " +
              "NAME 'testDontRequireATAllowedByDCROC' SUP top STRUCTURAL " +
              "MUST cn X-ORIGIN 'EntrySchemaCheckingTestCase')",
         "-",
         "add: ditContentRules",
         "ditContentRules: ( testdontrequireatallowedbydcroc-oid " +
              "NAME 'testDontRequireATAllowedByDCR' MAY description " +
              "X-ORIGIN 'EntrySchemaCheckingTestCase' )");

    String[] args =
    {
      "-h", "127.0.0.1",
      "-p", String.valueOf(TestCaseUtils.getServerLdapPort()),
      "-D", "cn=Directory Manager",
      "-w", "password",
      "-f", path
    };

    assertEquals(LDAPModify.run(nullPrintStream(), System.err, args), 0);

    Entry e = TestCaseUtils.makeEntry(
         "dn: cn=test,o=test",
         "objectClass: top",
         "objectClass: testDontRequireATAllowedByDCROC",
         "cn: test");

    LocalizableMessageBuilder invalidReason = new LocalizableMessageBuilder();
    assertTrue(e.conformsToSchema(null, false, true, true, invalidReason),
               invalidReason.toString());
  }



  /**
   * Tests schema checking for an entry covered by a DIT content rule to
   * ensure that attributes prohibited by the DIT content rule are not allowed
   * even if they are allowed by the associated object classes.
   *
   * @throws  Exception  If an unexpected problem occurs.
   */
  @Test
  public void testDontAllowAttributeProhibitedByDCR()
         throws Exception
  {
    TestCaseUtils.initializeTestBackend(true);

    String path = TestCaseUtils.createTempFile(
         "dn: cn=schema",
         "changetype: modify",
         "add: objectClasses",
         "objectClasses:  ( testdontallowattributeprohibitedbydcroc-oid " +
              "NAME 'testDontAllowAttributeProhibitedByDCROC' SUP top " +
              "STRUCTURAL MUST cn MAY description " +
              "X-ORIGIN 'EntrySchemaCheckingTestCase')",
         "-",
         "add: ditContentRules",
         "ditContentRules: ( testdontallowattributeprohibitedbydcroc-oid " +
              "NAME 'testDontAllowAttributeProhibitedByDCR' NOT description " +
              "X-ORIGIN 'EntrySchemaCheckingTestCase' )");

    String[] args =
    {
      "-h", "127.0.0.1",
      "-p", String.valueOf(TestCaseUtils.getServerLdapPort()),
      "-D", "cn=Directory Manager",
      "-w", "password",
      "-f", path
    };

    assertEquals(LDAPModify.run(nullPrintStream(), System.err, args), 0);

    Entry e = TestCaseUtils.makeEntry(
         "dn: cn=test,o=test",
         "objectClass: top",
         "objectClass: testDontAllowAttributeProhibitedByDCROC",
         "cn: test",
         "description: foo");

    failOnlyForStrictEvaluation(e);
  }



  /**
   * Tests that an entry covered by a name form will be accepted if its
   * single-valued RDN component is compliant with that name form.
   *
   * @throws  Exception  If an unexpected problem occurs.
   */
  @Test
  public void testSatisfiesSingleValuedNameForm()
         throws Exception
  {
    TestCaseUtils.initializeTestBackend(true);

    String path = TestCaseUtils.createTempFile(
         "dn: cn=schema",
         "changetype: modify",
         "add: objectClasses",
         "objectClasses:  ( testsatisfiessinglevaluednameformoc-oid " +
              "NAME 'testSatisfiesSingleValuedNameFormOC' SUP top STRUCTURAL " +
              "MUST cn X-ORIGIN 'EntrySchemaCheckingTestCase')",
         "-",
         "add: nameForms",
         "nameForms: ( testsatisfiessinglevaluednameform-oid " +
              "NAME 'testSatisfiesSingleValuedNameForm' " +
              "OC testSatisfiesSingleValuedNameFormOC MUST cn " +
              "X-ORIGIN 'EntrySchemaCheckingTestCase' )");

    String[] args =
    {
      "-h", "127.0.0.1",
      "-p", String.valueOf(TestCaseUtils.getServerLdapPort()),
      "-D", "cn=Directory Manager",
      "-w", "password",
      "-f", path
    };

    assertEquals(LDAPModify.run(nullPrintStream(), System.err, args), 0);

    Entry e = TestCaseUtils.makeEntry(
         "dn: cn=test,o=test",
         "objectClass: top",
         "objectClass: testSatisfiesSingleValuedNameFormOC",
         "cn: test");

    LocalizableMessageBuilder invalidReason = new LocalizableMessageBuilder();
    assertTrue(e.conformsToSchema(null, false, true, true, invalidReason),
               invalidReason.toString());
  }



  /**
   * Tests that an entry covered by a name form will be rejected if its
   * single-valued RDN component violates that name form.
   *
   * @throws  Exception  If an unexpected problem occurs.
   */
  @Test
  public void testViolatesSingleValuedNameForm()
         throws Exception
  {
    TestCaseUtils.initializeTestBackend(true);

    String path = TestCaseUtils.createTempFile(
         "dn: cn=schema",
         "changetype: modify",
         "add: objectClasses",
         "objectClasses:  ( testviolatessinglevaluednameformoc-oid " +
              "NAME 'testViolatesSingleValuedNameFormOC' SUP top STRUCTURAL " +
              "MUST cn MAY description X-ORIGIN 'EntrySchemaCheckingTestCase')",
         "-",
         "add: nameForms",
         "nameForms: ( testviolatessinglevaluednameform-oid " +
              "NAME 'testViolatesSingleValuedNameForm' " +
              "OC testViolatesSingleValuedNameFormOC MUST cn " +
              "X-ORIGIN 'EntrySchemaCheckingTestCase' )");

    String[] args =
    {
      "-h", "127.0.0.1",
      "-p", String.valueOf(TestCaseUtils.getServerLdapPort()),
      "-D", "cn=Directory Manager",
      "-w", "password",
      "-f", path
    };

    assertEquals(LDAPModify.run(nullPrintStream(), System.err, args), 0);

    Entry e = TestCaseUtils.makeEntry(
         "dn: description=foo,o=test",
         "objectClass: top",
         "objectClass: testViolatesSingleValuedNameFormOC",
         "cn: test",
         "description: foo");

    failOnlyForStrictEvaluation(e);
  }



  /**
   * Tests that an entry covered by a name form will be rejected if its
   * multivalued RDN component violates that name form which only allows a
   * single value.
   *
   * @throws  Exception  If an unexpected problem occurs.
   */
  @Test
  public void testMVViolatesSingleValuedNameForm()
         throws Exception
  {
    TestCaseUtils.initializeTestBackend(true);

    String path = TestCaseUtils.createTempFile(
         "dn: cn=schema",
         "changetype: modify",
         "add: objectClasses",
         "objectClasses:  ( testmvviolatessinglevaluednameformoc-oid " +
              "NAME 'testMVViolatesSingleValuedNameFormOC' SUP top STRUCTURAL " +
              "MUST cn MAY description X-ORIGIN 'EntrySchemaCheckingTestCase')",
         "-",
         "add: nameForms",
         "nameForms: ( testmvviolatessinglevaluednameform-oid " +
              "NAME 'testMVViolatesSingleValuedNameForm' " +
              "OC testMVViolatesSingleValuedNameFormOC MUST cn " +
              "X-ORIGIN 'EntrySchemaCheckingTestCase' )");

    String[] args =
    {
      "-h", "127.0.0.1",
      "-p", String.valueOf(TestCaseUtils.getServerLdapPort()),
      "-D", "cn=Directory Manager",
      "-w", "password",
      "-f", path
    };

    assertEquals(LDAPModify.run(nullPrintStream(), System.err, args), 0);

    Entry e = TestCaseUtils.makeEntry(
         "dn: cn=test+description=foo,o=test",
         "objectClass: top",
         "objectClass: testMVViolatesSingleValuedNameFormOC",
         "cn: test",
         "description: foo");

    failOnlyForStrictEvaluation(e);
  }



  /**
   * Tests that an entry covered by a name form will not be rejected if its
   * single-valued RDN component violates that name form but the name form is
   * declared OBSOLETE.
   *
   * @throws  Exception  If an unexpected problem occurs.
   */
  @Test
  public void testViolatesSingleValuedObsoleteNameForm()
         throws Exception
  {
    TestCaseUtils.initializeTestBackend(true);

    String path = TestCaseUtils.createTempFile(
         "dn: cn=schema",
         "changetype: modify",
         "add: objectClasses",
         "objectClasses:  ( testviolatessinglevaluedobsoletenameformoc-oid " +
              "NAME 'testViolatesSingleValuedObsoleteNameFormOC' SUP top " +
              "STRUCTURAL MUST cn MAY description " +
              "X-ORIGIN 'EntrySchemaCheckingTestCase')",
         "-",
         "add: nameForms",
         "nameForms: ( testviolatessinglevaluedobsoletenameform-oid " +
              "NAME 'testViolatesSingleValuedObsoleteNameForm' OBSOLETE " +
              "OC testViolatesSingleValuedObsoleteNameFormOC MUST cn " +
              "X-ORIGIN 'EntrySchemaCheckingTestCase' )");

    String[] args =
    {
      "-h", "127.0.0.1",
      "-p", String.valueOf(TestCaseUtils.getServerLdapPort()),
      "-D", "cn=Directory Manager",
      "-w", "password",
      "-f", path
    };

    assertEquals(LDAPModify.run(nullPrintStream(), System.err, args), 0);

    Entry e = TestCaseUtils.makeEntry(
         "dn: description=foo,o=test",
         "objectClass: top",
         "objectClass: testViolatesSingleValuedObsoleteNameFormOC",
         "cn: test",
         "description: foo");

    LocalizableMessageBuilder invalidReason = new LocalizableMessageBuilder();
    assertTrue(e.conformsToSchema(null, false, true, true, invalidReason),
               invalidReason.toString());
  }



  /**
   * Tests that an entry covered by a name form will be accepted if its
   * multivalued RDN component is compliant with that name form which requires
   * multiple values.
   *
   * @throws  Exception  If an unexpected problem occurs.
   */
  @Test
  public void testSatisfiesRequiredMultiValuedNameForm()
         throws Exception
  {
    TestCaseUtils.initializeTestBackend(true);

    String path = TestCaseUtils.createTempFile(
         "dn: cn=schema",
         "changetype: modify",
         "add: objectClasses",
         "objectClasses:  ( testsatisfiesrequiredmultivaluednameformoc-oid " +
              "NAME 'testSatisfiesRequiredMultiValuedNameFormOC' SUP top " +
              "STRUCTURAL MUST cn MAY description " +
              "X-ORIGIN 'EntrySchemaCheckingTestCase')",
         "-",
         "add: nameForms",
         "nameForms: ( testsatisfiesrequiredmultivaluednameform-oid " +
              "NAME 'testSatisfiesRequiredMultiValuedNameForm' " +
              "OC testSatisfiesRequiredMultiValuedNameFormOC " +
              "MUST ( cn $ description ) " +
              "X-ORIGIN 'EntrySchemaCheckingTestCase' )");

    String[] args =
    {
      "-h", "127.0.0.1",
      "-p", String.valueOf(TestCaseUtils.getServerLdapPort()),
      "-D", "cn=Directory Manager",
      "-w", "password",
      "-f", path
    };

    assertEquals(LDAPModify.run(nullPrintStream(), System.err, args), 0);

    Entry e = TestCaseUtils.makeEntry(
         "dn: cn=test+description=foo,o=test",
         "objectClass: top",
         "objectClass: testSatisfiesRequiredMultiValuedNameFormOC",
         "cn: test",
         "description: foo");

    LocalizableMessageBuilder invalidReason = new LocalizableMessageBuilder();
    assertTrue(e.conformsToSchema(null, false, true, true, invalidReason),
               invalidReason.toString());
  }



  /**
   * Tests that an entry covered by a name form will be accepted if its
   * single-valued RDN component only contains one of the multiple required
   * attribute types.
   *
   * @throws  Exception  If an unexpected problem occurs.
   */
  @Test
  public void testViolatesRequiredMultiValuedNameForm()
         throws Exception
  {
    TestCaseUtils.initializeTestBackend(true);

    String path = TestCaseUtils.createTempFile(
         "dn: cn=schema",
         "changetype: modify",
         "add: objectClasses",
         "objectClasses:  ( testviolatesrequiredmultivaluednameformoc-oid " +
              "NAME 'testViolatesRequiredMultiValuedNameFormOC' SUP top " +
              "STRUCTURAL MUST cn MAY description " +
              "X-ORIGIN 'EntrySchemaCheckingTestCase')",
         "-",
         "add: nameForms",
         "nameForms: ( testviolatesrequiredmultivaluednameform-oid " +
              "NAME 'testViolatesRequiredMultiValuedNameForm' " +
              "OC testViolatesRequiredMultiValuedNameFormOC " +
              "MUST ( cn $ description ) " +
              "X-ORIGIN 'EntrySchemaCheckingTestCase' )");

    String[] args =
    {
      "-h", "127.0.0.1",
      "-p", String.valueOf(TestCaseUtils.getServerLdapPort()),
      "-D", "cn=Directory Manager",
      "-w", "password",
      "-f", path
    };

    assertEquals(LDAPModify.run(nullPrintStream(), System.err, args), 0);

    Entry e = TestCaseUtils.makeEntry(
         "dn: cn=test,o=test",
         "objectClass: top",
         "objectClass: testViolatesRequiredMultiValuedNameFormOC",
         "cn: test",
         "description: foo");

    failOnlyForStrictEvaluation(e);
  }



  /**
   * Tests that an entry covered by a name form will be accepted if its
   * single-valued RDN component is compliant with that name form which requires
   * one value but allows other values.
   *
   * @throws  Exception  If an unexpected problem occurs.
   */
  @Test
  public void testSVSatisfiesOptionalMultiValuedNameForm()
         throws Exception
  {
    TestCaseUtils.initializeTestBackend(true);

    String path = TestCaseUtils.createTempFile(
         "dn: cn=schema",
         "changetype: modify",
         "add: objectClasses",
         "objectClasses:  ( testsvsatisfiesoptionalmultivaluednameformoc-oid " +
              "NAME 'testSVSatisfiesOptionalMultiValuedNameFormOC' SUP top " +
              "STRUCTURAL MUST cn MAY description " +
              "X-ORIGIN 'EntrySchemaCheckingTestCase')",
         "-",
         "add: nameForms",
         "nameForms: ( testsvsatisfiesoptionalmultivaluednameform-oid " +
              "NAME 'testSVSatisfiesOptionalMultiValuedNameForm' " +
              "OC testSVSatisfiesOptionalMultiValuedNameFormOC MUST cn " +
              "MAY description X-ORIGIN 'EntrySchemaCheckingTestCase' )");

    String[] args =
    {
      "-h", "127.0.0.1",
      "-p", String.valueOf(TestCaseUtils.getServerLdapPort()),
      "-D", "cn=Directory Manager",
      "-w", "password",
      "-f", path
    };

    assertEquals(LDAPModify.run(nullPrintStream(), System.err, args), 0);

    Entry e = TestCaseUtils.makeEntry(
         "dn: cn=test,o=test",
         "objectClass: top",
         "objectClass: testSVSatisfiesOptionalMultiValuedNameFormOC",
         "cn: test",
         "description: foo");

    LocalizableMessageBuilder invalidReason = new LocalizableMessageBuilder();
    assertTrue(e.conformsToSchema(null, false, true, true, invalidReason),
               invalidReason.toString());
  }



  /**
   * Tests that an entry covered by a name form will be accepted if its
   * multivalued RDN component is compliant with that name form which requires
   * one value but allows other values.
   *
   * @throws  Exception  If an unexpected problem occurs.
   */
  @Test
  public void testMVSatisfiesOptionalMultiValuedNameForm()
         throws Exception
  {
    TestCaseUtils.initializeTestBackend(true);

    String path = TestCaseUtils.createTempFile(
         "dn: cn=schema",
         "changetype: modify",
         "add: objectClasses",
         "objectClasses:  ( testmvsatisfiesoptionalmultivaluednameformoc-oid " +
              "NAME 'testMVSatisfiesOptionalMultiValuedNameFormOC' SUP top " +
              "STRUCTURAL MUST cn MAY description " +
              "X-ORIGIN 'EntrySchemaCheckingTestCase')",
         "-",
         "add: nameForms",
         "nameForms: ( testmvsatisfiesoptionalmultivaluednameform-oid " +
              "NAME 'testMVSatisfiesOptionalMultiValuedNameForm' " +
              "OC testMVSatisfiesOptionalMultiValuedNameFormOC MUST cn " +
              "MAY description X-ORIGIN 'EntrySchemaCheckingTestCase' )");

    String[] args =
    {
      "-h", "127.0.0.1",
      "-p", String.valueOf(TestCaseUtils.getServerLdapPort()),
      "-D", "cn=Directory Manager",
      "-w", "password",
      "-f", path
    };

    assertEquals(LDAPModify.run(nullPrintStream(), System.err, args), 0);

    Entry e = TestCaseUtils.makeEntry(
         "dn: cn=test+description=foo,o=test",
         "objectClass: top",
         "objectClass: testMVSatisfiesOptionalMultiValuedNameFormOC",
         "cn: test",
         "description: foo");

    LocalizableMessageBuilder invalidReason = new LocalizableMessageBuilder();
    assertTrue(e.conformsToSchema(null, false, true, true, invalidReason),
               invalidReason.toString());
  }



  /**
   * Tests that an entry covered by a name form will be accepted if its
   * single-valued RDN component violates that name form which requires
   * one value but allows other values.
   *
   * @throws  Exception  If an unexpected problem occurs.
   */
  @Test
  public void testSVViolatesOptionalMultiValuedNameForm()
         throws Exception
  {
    TestCaseUtils.initializeTestBackend(true);

    String path = TestCaseUtils.createTempFile(
         "dn: cn=schema",
         "changetype: modify",
         "add: objectClasses",
         "objectClasses:  ( testsvviolatesoptionalmultivaluednameformoc-oid " +
              "NAME 'testSVViolatesOptionalMultiValuedNameFormOC' SUP top " +
              "STRUCTURAL MUST cn MAY description " +
              "X-ORIGIN 'EntrySchemaCheckingTestCase')",
         "-",
         "add: nameForms",
         "nameForms: ( testsvviolatesoptionalmultivaluednameform-oid " +
              "NAME 'testSVViolatesOptionalMultiValuedNameForm' " +
              "OC testSVViolatesOptionalMultiValuedNameFormOC MUST cn " +
              "MAY description X-ORIGIN 'EntrySchemaCheckingTestCase' )");

    String[] args =
    {
      "-h", "127.0.0.1",
      "-p", String.valueOf(TestCaseUtils.getServerLdapPort()),
      "-D", "cn=Directory Manager",
      "-w", "password",
      "-f", path
    };

    assertEquals(LDAPModify.run(nullPrintStream(), System.err, args), 0);

    Entry e = TestCaseUtils.makeEntry(
         "dn: description=foo,o=test",
         "objectClass: top",
         "objectClass: testSVViolatesOptionalMultiValuedNameFormOC",
         "cn: test",
         "description: foo");

    failOnlyForStrictEvaluation(e);
  }



  /**
   * Performs various tests to ensure that the server appropriately enforces DIT
   * structure rule constraints.
   *
   * @throws  Exception  If an unexpected problem occurs.
   */
  @Test
  public void testDITStructureRuleConstraints()
         throws Exception
  {
    TestCaseUtils.initializeTestBackend(true);

    String path = TestCaseUtils.createTempFile(
         "dn: cn=schema",
         "changetype: modify",
         "add: objectClasses",
         "objectClasses:  ( testditstructureruleconstraintssupoc-oid " +
              "NAME 'testDITStructureRuleConstraintsSupOC' SUP top " +
              "STRUCTURAL MUST ou X-ORIGIN 'SchemaBackendTestCase')",
         "objectClasses:  ( testditstructureruleconstraintssuboc-oid " +
              "NAME 'testDITStructureRuleConstraintsSubOC' SUP top " +
              "STRUCTURAL MUST cn X-ORIGIN 'SchemaBackendTestCase')",
         "-",
         "add: nameForms",
         "nameForms: ( testditstructureruleconstraintsupsnf-oid " +
              "NAME 'testDITStructureRuleConstraintsSupNF' " +
              "OC testDITStructureRuleConstraintsSupOC MUST ou " +
              "X-ORIGIN 'SchemaBackendTestCase' )",
         "nameForms: ( testditstructureruleconstraintsubsnf-oid " +
              "NAME 'testDITStructureRuleConstraintsSubNF' " +
              "OC testDITStructureRuleConstraintsSubOC MUST cn " +
              "X-ORIGIN 'SchemaBackendTestCase' )",
         "-",
         "add: ditStructureRules",
         "ditStructureRules: ( 999014 " +
              "NAME 'testDITStructureRuleConstraintsSup' " +
              "FORM testDITStructureRuleConstraintsSupNF " +
              "X-ORIGIN 'SchemaBackendTestCase' )",
         "ditStructureRules: ( 999015 " +
              "NAME 'testDITStructureRuleConstraintsSub' " +
              "FORM testDITStructureRuleConstraintsSubNF SUP 999014 " +
              "X-ORIGIN 'SchemaBackendTestCase' )");

    String[] args =
    {
      "-h", "127.0.0.1",
      "-p", String.valueOf(TestCaseUtils.getServerLdapPort()),
      "-D", "cn=Directory Manager",
      "-w", "password",
      "-f", path
    };

    assertEquals(LDAPModify.run(nullPrintStream(), System.err, args), 0);


    Entry e = TestCaseUtils.makeEntry(
         "dn: cn=child,ou=parent,o=test",
         "objectClass: top",
         "objectClass: testDITStructureRuleConstraintsSubOC",
         "cn: child");

    failOnlyForStrictEvaluation(e);


    path = TestCaseUtils.createTempFile(
         "dn: ou=parent,o=test",
         "changetype: add",
         "objectClass: top",
         "objectClass: testDITStructureRuleConstraintsSupOC",
         "ou: parent");

    args = new String[]
    {
      "-h", "127.0.0.1",
      "-p", String.valueOf(TestCaseUtils.getServerLdapPort()),
      "-D", "cn=Directory Manager",
      "-w", "password",
      "-f", path
    };

    assertEquals(LDAPModify.run(nullPrintStream(), System.err, args), 0);

    LocalizableMessageBuilder invalidReason = new LocalizableMessageBuilder();
    assertTrue(e.conformsToSchema(null, false, true, true, invalidReason),
               invalidReason.toString());


    e = TestCaseUtils.makeEntry(
         "dn: cn=not below valid parent,o=test",
         "objectClass: top",
         "objectClass: testDITStructureRuleConstraintsSubOC",
         "cn: not below valid parent");
    failOnlyForStrictEvaluation(e);

    System.setProperty("org.openidentityplatform.opendj.ERR_ENTRY_SCHEMA_VIOLATES_PARENT_DSR","yes");
    e = TestCaseUtils.makeEntry(
         "dn: cn=invalid entry below parent covered by DSR,ou=parent,o=test",
         "objectClass: top",
         "objectClass: device",
         "cn: invalid entry below parent covered by DSR");
    invalidReason = new LocalizableMessageBuilder();
    failOnlyForStrictEvaluation(e);
  }

  /**
   * Tests schema checking for an entry that includes an attribute not
   * defined in any objectClasses but the subtypes of the attribute are.
   *
   * @throws  Exception  If an unexpected problem occurs.
   */
  @Test
  public void testInvalidSuperiorAttribute()
         throws Exception
  {
    // The LDIF reader won't let us do this directly, so we have to hack around
    // it.
    Entry e = TestCaseUtils.makeEntry(
         "dn: uid=test.user,o=test",
         "objectClass: top",
         "objectClass: person",
         "objectClass: organizationalPerson",
         "objectClass: inetOrgPerson",
         "objectClass: account",
         "uid: test.user",
         "givenName: Test",
         "sn: User",
         "cn: Test User");

    e.addAttribute(Attributes.create("name", "foo"), new LinkedList<ByteString>());

    assertFalse(e.conformsToSchema(null, false, true, true, new LocalizableMessageBuilder()));
  }
}

