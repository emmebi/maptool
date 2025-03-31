package net.rptools.maptool.client.macro;

import static org.junit.jupiter.api.Assertions.*;

import java.net.URI;
import net.rptools.maptool.model.Token;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class MacroLocationTest {

  @Test
  void testParseMacroNameCampaign() {
    MacroLocation location = MacroLocation.parseMacroName("test@campaign", null, null);
    assertEquals("test", location.getName());
    assertEquals(MacroLocation.MacroSource.campaign, location.getSource());
    assertEquals("campaign", location.getLocation());
    assertNull(location.getUri());
  }

  @Test
  void testParseMacroNameGM() {
    MacroLocation location = MacroLocation.parseMacroName("test@gm", null, null);
    assertEquals("test", location.getName());
    assertEquals(MacroLocation.MacroSource.gm, location.getSource());
    assertEquals("gm", location.getLocation());
    assertNull(location.getUri());
  }

  @Test
  void testParseMacroNameToken() {
    Token mockToken = Mockito.mock(Token.class);
    Mockito.when(mockToken.getName()).thenReturn("mockToken");

    MacroLocation location = MacroLocation.parseMacroName("test@token", null, mockToken);
    assertEquals("test", location.getName());
    assertEquals(MacroLocation.MacroSource.token, location.getSource());
    assertEquals("mockToken", location.getLocation());
    assertNull(location.getUri());
  }

  @Test
  void testParseMacroNameLibToken() {
    MacroLocation location = MacroLocation.parseMacroName("test@lib:libName", null, null);
    assertEquals("test", location.getName());
    assertEquals(MacroLocation.MacroSource.libToken, location.getSource());
    assertEquals("lib:libName", location.getLocation());
    assertNull(location.getUri());
  }

  @Test
  void testParseMacroNameURI() {
    MacroLocation location = MacroLocation.parseMacroName("lib://host/path", null, null);
    assertEquals("path", location.getName());
    assertEquals(MacroLocation.MacroSource.uri, location.getSource());
    assertEquals("host", location.getLocation());
    assertNotNull(location.getUri());
    assertEquals(URI.create("lib://host/path"), location.getUri());
  }

  @Test
  void testParseInvalidNameUri() {
    MacroLocation location = MacroLocation.parseMacroName("$$::invalidUri", null, null);
    assertEquals("$$::invalidUri", location.getName());
    assertEquals(MacroLocation.MacroSource.unknown, location.getSource());
    assertEquals("", location.getLocation());
    assertNull(location.getUri());
  }

  @Test
  void testParseMacroNameUnknown() {
    MacroLocation location = MacroLocation.parseMacroName("unknownMacro", null, null);
    assertEquals("unknownMacro", location.getName());
    assertEquals(MacroLocation.MacroSource.unknown, location.getSource());
  }

  @Test
  void testParseMacroNameAtThis() {
    Token mockToken = Mockito.mock(Token.class);
    Mockito.when(mockToken.getName()).thenReturn("mockToken");

    MacroLocation location = MacroLocation.parseMacroName("test@this", null, mockToken);
    assertEquals("test", location.getName());
    assertEquals(MacroLocation.MacroSource.token, location.getSource());
    assertEquals("mockToken", location.getLocation());
    assertNull(location.getUri());
  }
}
