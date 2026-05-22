package com.shooter.client;

import com.shooter.network.ChatMessage;
import com.shooter.network.NetworkMessage;
import com.shooter.network.MessageType;
import com.shooter.shared.util.Constants;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;

/**
 * Custom modern scrollable chat overlay that renders dynamically on any Graphics2D context.
 * Features glassmorphic transparent background, golden system announcements, blue player tags,
 * micro-animated blinking cursor, and automatic inactivity fade-out.
 */
public class ChatPanel {
    private final GameClient client;
    private boolean active = false;
    private boolean justActivated = false;
    private String inputBuffer = "";
    private final List<ChatMessage> messages = new ArrayList<>();
    
    // Animation/Fading parameters
    private int cursorTick = 0;
    private long lastMessageTime = 0;
    private static final long CHAT_FADE_DELAY_MS = 6000; // Chat history stays visible for 6 seconds after inactive/new message
    
    public ChatPanel(GameClient client) {
        this.client = client;
        
        // Listen to incoming chat messages
        if (client != null) {
            client.setChatMessageListener(this::addMessage);
        }
    }
    
    public synchronized void addMessage(ChatMessage msg) {
        messages.add(msg);
        if (messages.size() > 50) {
            messages.remove(0);
        }
        lastMessageTime = System.currentTimeMillis();
    }
    
    public boolean isActive() {
        return active;
    }
    
    public void setActive(boolean active) {
        this.active = active;
        if (active) {
            inputBuffer = "";
            lastMessageTime = System.currentTimeMillis();
            justActivated = true;
        }
    }
    
    public void tick() {
        cursorTick = (cursorTick + 1) % 40; // Blink cursor speed
    }
    
    public boolean handleKeyPressed(int keyCode) {
        if (!active) {
            if (keyCode == KeyEvent.VK_T) {
                setActive(true);
                return true; // consumed
            }
            return false;
        }
        
        if (keyCode == KeyEvent.VK_ESCAPE) {
            setActive(false);
            return true; // consumed
        }
        
        if (keyCode == KeyEvent.VK_ENTER) {
            String text = inputBuffer.trim();
            if (!text.isEmpty()) {
                sendChat(text);
            }
            setActive(false);
            return true; // consumed
        }
        
        if (keyCode == KeyEvent.VK_BACK_SPACE) {
            if (inputBuffer.length() > 0) {
                inputBuffer = inputBuffer.substring(0, inputBuffer.length() - 1);
            }
            lastMessageTime = System.currentTimeMillis();
            return true; // consumed
        }
        
        return false;
    }
    
    public boolean handleKeyTyped(char keyChar) {
        if (!active) return false;
        
        // Skip the immediate character typed if we just activated (e.g. the 't' that opened the chat)
        if (justActivated) {
            justActivated = false;
            if (Character.toLowerCase(keyChar) == 't') {
                return true;
            }
        }
        
        // Only append printable characters (ASCII 32 to 126, or other unicode characters)
        if (keyChar >= 32 && keyChar != 127) {
            if (inputBuffer.length() < 60) { // Limit length
                inputBuffer += keyChar;
                lastMessageTime = System.currentTimeMillis();
            }
            return true;
        }
        return false;
    }
    
    private void sendChat(String text) {
        if (client == null || !client.isConnectedToServer()) return;
        
        String sender = "Player " + (client.getMyPlayerId() + 1);
        ChatMessage chatMsg = new ChatMessage(sender, text, System.currentTimeMillis());
        client.sendMessage(new NetworkMessage(MessageType.CHAT, client.getMyPlayerId(), chatMsg));
    }
    
    public void render(Graphics2D g) {
        tick(); // Advance cursor blink animation on every repaint!
        
        long now = System.currentTimeMillis();
        boolean shouldShowHistory = active || (now - lastMessageTime < CHAT_FADE_DELAY_MS);
        
        if (!shouldShowHistory && messages.isEmpty()) {
            return;
        }
        
        // Font setup
        g.setFont(new Font("SansSerif", Font.PLAIN, 14));
        FontMetrics fm = g.getFontMetrics();
        int lineHeight = fm.getHeight() + 4;
        
        // We show up to 6 last messages
        List<ChatMessage> visibleMessages;
        synchronized (this) {
            int start = Math.max(0, messages.size() - 6);
            visibleMessages = new ArrayList<>(messages.subList(start, messages.size()));
        }
        
        int boxWidth = 350;
        int maxVisibleLines = visibleMessages.size();
        int historyHeight = maxVisibleLines * lineHeight;
        int inputHeight = active ? lineHeight + 10 : 0;
        int padding = 12;
        int totalHeight = historyHeight + inputHeight + (maxVisibleLines > 0 ? padding * 2 : padding);
        
        int x = 20;
        int y = Constants.SCREEN_HEIGHT - totalHeight - 40; // Renders beautifully at bottom left
        
        // Calculate transparency fade out if inactive and time elapsed
        int alpha = 160;
        if (!active && (now - lastMessageTime > CHAT_FADE_DELAY_MS - 1000)) {
            long remaining = CHAT_FADE_DELAY_MS - (now - lastMessageTime);
            alpha = (int) (160 * (remaining / 1000f));
            if (alpha < 0) alpha = 0;
        }
        
        if (alpha <= 0) return;
        
        // Draw Glassmorphic Background Box
        g.setColor(new Color(15, 15, 22, alpha));
        g.fillRoundRect(x, y, boxWidth, totalHeight, 10, 10);
        
        // Border
        g.setColor(new Color(255, 255, 255, alpha / 4));
        g.drawRoundRect(x, y, boxWidth, totalHeight, 10, 10);
        
        int currentY = y + padding + fm.getAscent();
        
        // Draw Chat History
        for (ChatMessage msg : visibleMessages) {
            String senderPart = "[" + msg.getSender() + "]: ";
            String textPart = msg.getText();
            
            // Format sender nicely depending on if it is system message
            if ("System".equalsIgnoreCase(msg.getSender())) {
                g.setColor(new Color(255, 215, 0, alpha + 95)); // gold/yellow for system
            } else {
                g.setColor(new Color(129, 212, 250, alpha + 95)); // blue for sender
            }
            g.drawString(senderPart, x + padding, currentY);
            
            int senderWidth = fm.stringWidth(senderPart);
            g.setColor(new Color(245, 245, 247, alpha + 95)); // soft white for text
            g.drawString(textPart, x + padding + senderWidth, currentY);
            
            currentY += lineHeight;
        }
        
        // Draw Input Line if active
        if (active) {
            currentY += 4;
            // Draw a separator line
            g.setColor(new Color(255, 255, 255, 40));
            g.drawLine(x + padding, currentY - fm.getAscent() - 2, x + boxWidth - padding, currentY - fm.getAscent() - 2);
            
            String prompt = "[Chat]: ";
            g.setColor(new Color(244, 143, 177, 255)); // lovely pink prompt
            g.drawString(prompt, x + padding, currentY);
            
            int promptWidth = fm.stringWidth(prompt);
            g.setColor(Color.WHITE);
            g.drawString(inputBuffer, x + padding + promptWidth, currentY);
            
            // Blinking cursor
            if (cursorTick < 20) {
                int cursorX = x + padding + promptWidth + fm.stringWidth(inputBuffer);
                g.drawLine(cursorX, currentY - fm.getAscent(), cursorX, currentY + fm.getDescent());
            }
        }
    }
}
