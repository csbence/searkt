#!/usr/bin/env python3

import random
import requests
from flask import Flask, request

app = Flask(__name__)


@app.route('/workspace1', methods=['POST'])
def index():
    print('Proxy: Incoming request')
    data = list(request.form.items())[0][0]
    print('Proxy: data: {}'.format(data))
    r = requests.post("http://localhost:8080/workspace1?operation=updateGraph",
                      data='{"an":{"X":{"label":"Streaming Node X"}}}')
    print(r.text)
    response = requests.post("http://localhost:8080/workspace1", data=data, params={'operation': 'updateGraph'})
    return response.text


def generate_structured_nodes():
    ids = []
    all_data = ''
    for x in range(10):
        for y in range(10):
            for z in range(10):
                id = x + y * 10 + z * 100
                data = '{{"an":{{"{}":{{"label":"N", "x":"{}", "y":"{}", "z":"{}"}}}}}}\r\n'.format(id,
                                                                                                    x * 100,
                                                                                                    y * 100,
                                                                                                    z * 100)

                ids.append(id)
                all_data += data

    for i in range(100):
        source, target = random.sample(set(ids), 2)
        weight = random.randint(1, 9)
        data = '{{"ae":{{"{}":{{"source":"{}", "target":"{}", "directed":true, "weight":{}}}}}}}\r\n'.format(
            str(source) + '-' + str(target), source, target, weight)
        all_data += data

    print(len(all_data))

    r = requests.post("http://localhost:8080/workspace1?operation=updateGraph", data=all_data)

    print(r.text)

    print('done')


if __name__ == '__main__':
    generate_structured_nodes()
    # app.run(debug=True)
