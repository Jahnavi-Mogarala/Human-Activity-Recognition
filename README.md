# MotionShield

## Privacy-Preserving Human Activity Recognition Using Smartphone Sensors

MotionShield is a Human Activity Recognition (HAR) project that uses
smartphone accelerometer and gyroscope data to recognize human activities.

The current implementation uses the UCI-HAR dataset and a Bi-LSTM with
Temporal Attention for activity classification. The project includes the
data preparation, validation, training and evaluation pipeline.

---

## Aim

The aim of MotionShield is to build a reliable smartphone-based activity
recognition system using motion sensor data.

The project focuses on:

- Combining accelerometer and gyroscope data
- Processing sensor data in fixed time windows
- Keeping subjects separate during training and testing
- Avoiding data leakage
- Using a Bi-LSTM with Temporal Attention for activity classification
- Preparing the model for future smartphone-based inference

---

## Dataset

The current implementation uses the **UCI Human Activity Recognition Using
Smartphones** dataset.

| Property | Details |
|---|---|
| Subjects | 30 |
| Activities | 6 |
| Sensor channels | 6 |
| Sampling rate | 50 Hz |
| Window size | 128 samples |
| Total windows | 10,299 |

### Activities

- WALKING
- WALKING_UPSTAIRS
- WALKING_DOWNSTAIRS
- SITTING
- STANDING
- LAYING

### Sensor Channels

```text
acc_x
acc_y
acc_z
gyro_x
gyro_y
gyro_z
