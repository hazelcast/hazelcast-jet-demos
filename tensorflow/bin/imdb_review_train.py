import tensorflow as tf
from tensorflow import keras
import shutil
import sys

imdb = keras.datasets.imdb
num_words = 10000

print("loading data...")
(train_data, train_labels), (test_data, test_labels) = imdb.load_data(num_words=num_words)

print('padding reviews to fixed length...')
train_data = keras.preprocessing.sequence.pad_sequences(train_data,
                                                        value=0,
                                                        padding='post',
                                                        maxlen=256)
test_data = keras.preprocessing.sequence.pad_sequences(test_data,
                                                       value=0,
                                                       padding='post',
                                                       maxlen=256)

model = keras.Sequential()
model.add(keras.layers.Embedding(num_words, 16))
model.add(keras.layers.GlobalAveragePooling1D())
model.add(keras.layers.Dense(16, activation=tf.nn.relu))
model.add(keras.layers.Dense(1, activation=tf.nn.sigmoid))

model.summary()

model.compile(optimizer='adam',
              loss='binary_crossentropy',
              metrics=['accuracy'])

x_val = train_data[:10000]
partial_x_train = train_data[10000:]

y_val = train_labels[:10000]
partial_y_train = train_labels[10000:]

# train the model
print('training...')
history = model.fit(partial_x_train,
                    partial_y_train,
                    epochs=40,
                    batch_size=512,
                    validation_data=(x_val, y_val),
                    verbose=1)

print('training done.')

# save the model
sess = tf.keras.backend.get_session()
dataDir = sys.path[0] + "/../data"
modelDir = dataDir + "/model/1"
shutil.rmtree(modelDir, ignore_errors=True)
tf.saved_model.simple_save(sess, modelDir,
                           inputs={"input_review": model.input},
                           outputs={t.name:t for t in model.outputs})
print("Model saved to " + modelDir)
