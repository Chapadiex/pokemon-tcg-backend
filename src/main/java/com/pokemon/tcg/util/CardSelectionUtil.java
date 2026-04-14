package com.pokemon.tcg.util;

import com.pokemon.tcg.model.game.CardData;

import java.util.List;
import java.util.Map;

public final class CardSelectionUtil {

    private CardSelectionUtil() {
    }

    public static Integer extractHandIndex(Map<String, Object> data) {
        if (data == null || data.isEmpty()) {
            return null;
        }

        Integer index = toInteger(data.get("handIndex"));
        if (index != null) return index;
        index = toInteger(data.get("handPosition"));
        if (index != null) return index;
        index = toInteger(data.get("cardIndex"));
        if (index != null) return index;
        return toInteger(data.get("sourceHandIndex"));
    }

    public static CardData findInHand(List<CardData> hand, String cardId, Integer handIndex) {
        if (hand == null || hand.isEmpty() || cardId == null || cardId.isBlank()) {
            return null;
        }

        if (handIndex != null) {
            if (handIndex < 0 || handIndex >= hand.size()) {
                return null;
            }
            CardData indexedCard = hand.get(handIndex);
            if (indexedCard != null && cardId.equals(indexedCard.getId())) {
                return indexedCard;
            }
            return null;
        }

        for (CardData card : hand) {
            if (card != null && cardId.equals(card.getId())) {
                return card;
            }
        }

        return null;
    }

    public static boolean removeFromHand(List<CardData> hand, String cardId, Integer handIndex) {
        if (hand == null || hand.isEmpty() || cardId == null || cardId.isBlank()) {
            return false;
        }

        if (handIndex != null) {
            if (handIndex < 0 || handIndex >= hand.size()) {
                return false;
            }
            CardData indexedCard = hand.get(handIndex);
            if (indexedCard == null || !cardId.equals(indexedCard.getId())) {
                return false;
            }
            hand.remove((int) handIndex);
            return true;
        }

        return removeFirstById(hand, cardId);
    }

    public static boolean removeFirstById(List<CardData> cards, String cardId) {
        if (cards == null || cards.isEmpty() || cardId == null || cardId.isBlank()) {
            return false;
        }

        for (int i = 0; i < cards.size(); i++) {
            CardData card = cards.get(i);
            if (card != null && cardId.equals(card.getId())) {
                cards.remove(i);
                return true;
            }
        }

        return false;
    }

    private static Integer toInteger(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value instanceof String text) {
            try {
                return Integer.parseInt(text.trim());
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }
}
