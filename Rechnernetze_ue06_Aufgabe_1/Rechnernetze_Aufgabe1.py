import matplotlib.pyplot as plt
import numpy as np

# Grafik zu TCP Reno
fig, ax = plt.subplots()
x = np.linspace(0, 100, 1000)
y = np.piecewise(x, [x < 30, (x >= 30) & (x < 60), (x >= 60) & (x < 90), x >= 90], 
                 [lambda x: 0.5*x, lambda x: 15 + 0.1*(x-30), lambda x: 15, lambda x: 7.5 + 0.05*(x-90)])

ax.plot(x, y, label='Congestion Window')
ax.axvline(x=30, color='r', linestyle='--', label='Paketverlust')
ax.axvline(x=60, color='g', linestyle='--', label='Fast Recovery End')
ax.axvline(x=90, color='b', linestyle='--', label='Neue Stausituation')
ax.set_xlabel('Zeit')
ax.set_ylabel('Congestion Window Größe')
ax.legend()
plt.title('TCP Reno Verhalten')
plt.grid(True)

# Speichern der Grafik
plt.savefig("tcp_reno_diagram.png")
plt.show()
