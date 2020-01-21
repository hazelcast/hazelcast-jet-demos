import h2o
h2o.init()

#import public data set
# url = "https://h2o-public-test-data.s3.amazonaws.com/smalldata/wisc/wisc-diag-breast-cancer-shuffled.csv"
local_url = "../input/wisc-diag-breast-cancer-shuffled.csv"
data = h2o.import_file(local_url)

#split data into train and test frames and print summary
train, test = data.split_frame([0.8])
train.summary()

from h2o.estimators.gbm import H2OGradientBoostingEstimator

y = 'diagnosis'
x = data.columns
del x[0:1]

model = H2OGradientBoostingEstimator(distribution='bernoulli',
                                    ntrees=100,
                                    max_depth=4,
                                    learn_rate=0.1)
model.train(x=x, y=y, training_frame=train, validation_frame=test)
print model

p = model.predict(test)
print p

model.model_performance(test)

modelfile = model.download_mojo(path="../model/")
print("Model saved to " + modelfile)
h2o.export_file(test, force=True, path="../input/validation-input.csv")
print("Validation set saved as ../input/validation-input.csv")