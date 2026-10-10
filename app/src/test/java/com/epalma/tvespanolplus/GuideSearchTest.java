package com.epalma.tvespanolplus;

import static org.junit.Assert.*;
import org.junit.Test;

public class GuideSearchTest {
  @Test public void findsBarcelonaByFullName() {
    assertTrue(GuideSearch.matches("FC Barcelona", "Barcelona vs Getafe", "LaLiga", "Deportes 1"));
  }
  @Test public void findsBarcelonaByBarcaAlias() {
    assertTrue(GuideSearch.matches("Barça", "Real Betis - FC Barcelona", "Partido", "Canal Fútbol"));
  }
  @Test public void searchesDescriptionAndChannel() {
    assertTrue(GuideSearch.matches("champions", "Previa", "UEFA Champions League", "Fútbol HD"));
  }
  @Test public void rejectsUnrelatedEvent() {
    assertFalse(GuideSearch.matches("FC Barcelona", "Inter vs Milan", "Serie A", "Fútbol Italia"));
  }
}