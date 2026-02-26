import socket

def start_server():
    s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    s.bind(('localhost', 9000))
    s.listen(1)
    print("MOCK SERVER: Listening on port 9000")
    
    while True:
        conn, addr = s.accept()
        print(f"MOCK SERVER: Connected by {addr}")
        try:
            while True:
                data = conn.recv(1024)
                if not data:
                    break
                print(f"MOCK SERVER: Received {len(data)} bytes: {data.hex()}")
                
                # Simple ISO8583 response (0100 -> 0110)
                if b"0100" in data:
                    resp = data.replace(b"0100", b"0110")
                    # Add field 39 = 00 if possible, but let's just send back what we got with 0110
                    conn.sendall(resp)
                    print("MOCK SERVER: Sent response")
        except Exception as e:
            print(f"MOCK SERVER: Error: {e}")
        finally:
            conn.close()

if __name__ == "__main__":
    start_server()
