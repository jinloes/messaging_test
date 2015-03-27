import pika
import logging

logging.basicConfig()
connection = pika.BlockingConnection(pika.ConnectionParameters('localhost'))
channel = connection.channel()

channel.exchange_declare(exchange='spring-boot-exchange', type='topic', durable=True)

channel.basic_publish(exchange='spring-boot-exchange', routing_key='spring-boot',
                      properties=pika.BasicProperties(content_type='text/plain'), body='Hello from python!')
print " [x] Sent 'Hello from python!'"
connection.close()