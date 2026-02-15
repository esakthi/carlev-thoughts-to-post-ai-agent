from flask import Flask, jsonify, request
from flask_cors import CORS

app = Flask(__name__)
CORS(app)

@app.route('/api/oauth/linkedin/status', methods=['GET'])
def linkedin_status():
    return jsonify({"authorized": False})

@app.route('/api/oauth/linkedin/authorize', methods=['GET'])
def linkedin_authorize():
    return jsonify({
        "authorizationUrl": "https://www.linkedin.com/oauth/v2/authorization?mock=true",
        "state": "user-123"
    })

if __name__ == '__main__':
    app.run(port=8080)
