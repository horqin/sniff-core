## 定义
REDIS_HOST = 'centos'
N, M = 24, 100


## 模型
from pytorch_lightning.core.module import LightningModule
import torch

class Model(LightningModule):
    def __init__(self):
        super().__init__()
        self.embed = torch.nn.Embedding(256 + 1, 128)
        self.rnn_1 = torch.nn.LSTM(128 * 1, 128, bidirectional=True, batch_first=True)
        self.cnn_1 = torch.nn.Conv1d(128 * 3, 128 * 3, 1)
        self.alpha = torch.nn.Parameter(torch.rand((128 * 3)))
        self.rnn_2 = torch.nn.LSTM(128 * 3, 128, bidirectional=True, batch_first=True)
        self.att_1 = torch.nn.Linear(128 * 2, 1)
        self.fc = torch.nn.Linear(128 * 2, 5)
    def forward(self, x):
        N, H, W = x.shape
        x = self.embed(x)
        x = x.reshape(N * H, W, -1)
        h, _ = self.rnn_1(x)
        x = torch.cat([x, h], dim=2)
        x = self.cnn_1(x.permute(0, 2, 1)).permute(0, 2, 1)
        alpha = torch.sigmoid(self.alpha)
        x = torch.mul(x.mean(dim=1), alpha) + torch.mul(x.max(dim=1).values, 1 - alpha)
        x = x.reshape(N, H, -1)
        h, _ = self.rnn_2(x)
        x = torch.tanh(torch.bmm(torch.softmax(self.att_1(torch.tanh(h)).squeeze(2), dim=1).unsqueeze(1), h).squeeze(1))
        return torch.argmax(self.fc(x), dim=1)


## 服务
from flask import Flask, jsonify

# redis
from redis import Redis
redis = Redis(host=REDIS_HOST, port=6379)
# model
model = Model().load_from_checkpoint('data/model.chk')
# convert
def convert(session):
    matrix = [[0] * M for _ in range(N)]
    n = len(session)
    for i in range(min(n, N)):
        m = len(session[i])
        for j in range(min(m, M)):
            matrix[i][j] = session[i][j]
    return matrix
# flask
app = Flask(__name__)
@app.route('/<string:session>')
def forecast(session):
    session = redis.zrevrange('session::' + session, 0, N-1)
    pred = int(model(torch.LongTensor([convert(session)]))[0])
    return jsonify({ 'data': pred })

app.run(host='0.0.0.0', debug=True)

