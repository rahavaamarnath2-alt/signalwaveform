
import javax.swing.*;
import java.awt.*;

public class SignalWaveformGenerator extends JFrame {

    JTextField frequencyField, amplitudeField, phaseField, timeScaleField;
    JComboBox<String> waveType;
    JButton startButton, stopButton, updateButton, resetButton;

    WavePanel wavePanel;
    Timer timer;

    double frequency = 1000;
    double amplitude = 5;
    double phase = 0;
    double timeScale = 0.5;
    double position = 0;

    boolean running = true;

    public SignalWaveformGenerator() {
        setTitle("Basic Signal Waveform Generator");
        setSize(950, 650);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(5, 5));

        JPanel controls = new JPanel(new GridLayout(3, 4, 8, 8));
        controls.setBorder(BorderFactory.createTitledBorder("Signal Settings"));

        frequencyField = new JTextField("1k");
        amplitudeField = new JTextField("5");
        phaseField = new JTextField("0");
        timeScaleField = new JTextField("0.5");

        waveType = new JComboBox<>(
                new String[]{"Sine", "Square", "Triangle", "Sawtooth"}
        );

        controls.add(new JLabel("Waveform:"));
        controls.add(waveType);
        controls.add(new JLabel("Frequency (Hz/k/M):"));
        controls.add(frequencyField);

        controls.add(new JLabel("Amplitude (V):"));
        controls.add(amplitudeField);
        controls.add(new JLabel("Phase (degrees):"));
        controls.add(phaseField);

        controls.add(new JLabel("Time Scale (ms/div):"));
        controls.add(timeScaleField);

        updateButton = new JButton("Update Signal");
        resetButton = new JButton("Reset");
        controls.add(updateButton);
        controls.add(resetButton);

        add(controls, BorderLayout.NORTH);

        wavePanel = new WavePanel();
        add(wavePanel, BorderLayout.CENTER);

        JPanel buttons = new JPanel();

        startButton = new JButton("Start");
        stopButton = new JButton("Stop");

        buttons.add(startButton);
        buttons.add(stopButton);

        add(buttons, BorderLayout.SOUTH);

        updateButton.addActionListener(e -> updateSignal());

        resetButton.addActionListener(e -> resetSignal());

        startButton.addActionListener(e -> {
            running = true;
        });

        stopButton.addActionListener(e -> {
            running = false;
        });

        timer = new Timer(30, e -> {
            if (running) {
                position += 0.01;
                position %= 1.0;
                wavePanel.repaint();
            }
        });

        timer.start();
        setVisible(true);
    }

    void updateSignal() {
        try {
            frequency = parseFrequency(frequencyField.getText().trim());
            amplitude = Double.parseDouble(amplitudeField.getText().trim());
            phase = Double.parseDouble(phaseField.getText().trim());
            timeScale = Double.parseDouble(timeScaleField.getText().trim());

            if (frequency <= 0 || frequency > 1_000_000_000) {
                throw new IllegalArgumentException(
                        "Frequency must be positive and within the supported range."
                );
            }

            if (amplitude <= 0 || amplitude > 50) {
                throw new IllegalArgumentException(
                        "Amplitude must be greater than 0 and at most 50 V."
                );
            }

            if (timeScale <= 0 || timeScale > 1000000) {
                throw new IllegalArgumentException(
                        "Time scale must be positive."
                );
            }

            phase %= 360;

            wavePanel.repaint();

        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(
                    this,
                    "Enter valid numeric values.",
                    "Input Error",
                    JOptionPane.ERROR_MESSAGE
            );
        } catch (IllegalArgumentException ex) {
            JOptionPane.showMessageDialog(
                    this,
                    ex.getMessage(),
                    "Input Error",
                    JOptionPane.ERROR_MESSAGE
            );
        }
    }

    double parseFrequency(String text) {
        text = text.trim().toLowerCase();

        double multiplier = 1;

        if (text.endsWith("k")) {
            multiplier = 1000;
            text = text.substring(0, text.length() - 1);
        } else if (text.endsWith("m")) {
            multiplier = 1_000_000;
            text = text.substring(0, text.length() - 1);
        }

        return Double.parseDouble(text) * multiplier;
    }

    void resetSignal() {
        frequencyField.setText("1k");
        amplitudeField.setText("5");
        phaseField.setText("0");
        timeScaleField.setText("0.5");
        waveType.setSelectedIndex(0);

        frequency = 1000;
        amplitude = 5;
        phase = 0;
        timeScale = 0.5;
        position = 0;
        running = true;

        wavePanel.repaint();
    }

    class WavePanel extends JPanel {

        WavePanel() {
            setBackground(Color.WHITE);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);

            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(
                    RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON
            );

            int left = 65;
            int right = getWidth() - 25;
            int top = 30;
            int bottom = getHeight() - 55;

            int width = right - left;
            int height = bottom - top;

            if (width <= 0 || height <= 0) {
                return;
            }

            // Automatically select the voltage range.
            double maxVoltage = Math.min(
                    50,
                    Math.max(5, Math.ceil(amplitude / 5.0) * 5.0)
            );

            // Draw the CRO grid: 10 horizontal divisions.
            g2.setColor(new Color(220, 220, 220));

            for (int i = 0; i <= 10; i++) {
                int x = left + i * width / 10;
                g2.drawLine(x, top, x, bottom);
            }

            // Draw 8 vertical divisions.
            for (int i = 0; i <= 8; i++) {
                int y = top + i * height / 8;
                g2.drawLine(left, y, right, y);
            }

            // Draw the axes.
            g2.setColor(Color.BLACK);
            g2.drawRect(left, top, width, height);

            // Draw voltage labels.
            for (int i = 0; i <= 8; i++) {
                double voltage = maxVoltage - i * (2 * maxVoltage / 8.0);
                int y = top + i * height / 8;

                String label = String.format("%.2f V", voltage);
                g2.drawString(label, 5, y + 5);
            }

            // Draw time labels based on milliseconds per division.
            for (int i = 0; i <= 10; i++) {
                double time = i * timeScale;
                int x = left + i * width / 10;

                String label = String.format("%.1f", time);
                g2.drawString(label, x - 10, bottom + 18);
            }

            g2.drawString(
                    "Time (ms)  |  " + timeScale + " ms/div",
                    left + width / 2 - 65,
                    getHeight() - 12
            );

            g2.drawString(
                    "Voltage (V)",
                    5,
                    15
            );

            // Total time represented by the screen.
            double timeWindow = timeScale * 10.0 / 1000.0;

            // Number of cycles visible across the screen.
            double visibleCycles = frequency * timeWindow;

            // Avoid excessive drawing work at extremely high frequencies.
            visibleCycles = Math.min(visibleCycles, 2000);

            double phaseRadians = Math.toRadians(phase);

            g2.setColor(new Color(0, 100, 220));
            g2.setStroke(new BasicStroke(2));

            int previousX = left;
            int previousY = bottom - height / 2;

            for (int px = 0; px <= width; px++) {
                double fraction = px / (double) width;

                double angle = 2 * Math.PI *
                        (visibleCycles * fraction + position)
                        + phaseRadians;

                double value = getWaveValue(angle);

                // Convert voltage into screen coordinates.
                int x = left + px;
                int y = (int) (
                        top + height / 2.0
                        - (value / maxVoltage) * (height / 2.0)
                );

                if (px > 0) {
                    g2.drawLine(previousX, previousY, x, y);
                }

                previousX = x;
                previousY = y;
            }

            g2.setStroke(new BasicStroke(1));
        }

        double getWaveValue(double angle) {
            double cycle = angle / (2 * Math.PI);
            cycle = cycle - Math.floor(cycle);

            switch ((String) waveType.getSelectedItem()) {
                case "Square":
                    return Math.sin(angle) >= 0 ? amplitude : -amplitude;

                case "Triangle":
                    return amplitude *
                            (1 - 4 * Math.abs(cycle - 0.5));

                case "Sawtooth":
                    return amplitude * (2 * cycle - 1);

                default:
                    return amplitude * Math.sin(angle);
            }
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() ->
                new SignalWaveformGenerator()
        );
    }
}
