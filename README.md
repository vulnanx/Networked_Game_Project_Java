# CMSC 137 — Project: Holy Shot!
> A 2D Multiplayer Co-op Shooter 

## Overview
A pixelated 2D top-down PvE co-op shooter built in Java.  
Up to four players cooperate to survive 5 rounds of increasingly difficult supernatural enemy waves.  
Built from scratch with a custom game loop — no full game engines used.

**Theme:** Players are elite spirit-cleansing hunters battling supernatural Filipino folklore entities across haunted arenas.

---

## How to Run

### Prerequisites
- Java 17 or higher
- Maven (optional, for building)

### Build the Project
```bash
# Compile all Java files into the 'out' directory
javac -d out $(find src/main/java -name "*.java")
```

**Step 1: Compile the project (Host Machine)**
```bash
# Terminal 1 — Server
java -cp out:src/main/resources com.shooter.server.GameServer

java -cp "out;src/main/resources" com.shooter.server.GameServer
```
**Step 2: Find the host's local IP address: (Host Machine)**
```bash
# Linux / WSL
ip addr show | grep "inet " | grep -v 127.0.0.1
# Windows
ipconfig
```
**Step 3: Compile the project (Client Machine)**
```bash
# Terminal 1 — Server
java -cp out:src/main/resources com.shooter.client.GameClient

java -cp "out;src/main/resources" com.shooter.client.GameClient

```
**Step 4: Start The Game**

### Common Problems
1.  **Can't connect** — Make sure all PCs are on the same Wi-Fi/LAN. Port 5000 must not be blocked by a firewall
2.  **Windows Firewall blocks it** — Allow Java through Windows Defender Firewall, or run: `netsh advfirewall firewall add rule name="HolyShot" protocol=TCP dir=in localport=5000 action=allow`
3.  **WSL networking issue** — WSL has its own internal IP — you may need to use the Windows IP (from `ipconfig`), not the WSL IP
4.  **ClassNotFoundException on connect** — Both sides must be compiled from the exact same source code. Recompile on all machines.

## Team

| Members |
|--------|
| Mirano, Christel| 
| Garcia, Sophia Ysabel | 
| Castillo, Geastin|
